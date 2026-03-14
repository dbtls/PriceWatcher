package com.example.pricewatch.domain.recommendation.service;

import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.recommendation.dto.ProductRecommendationsRes;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 연관 상품 추천 서비스.
 */
@Service
@RequiredArgsConstructor
public class RelatedProductService {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+");
    private static final Set<String> NOISE_TOKENS = Set.of(
            "THE", "A", "AN", "OF", "AND", "FOR", "WITH", "IN", "ON",
            "상품", "공용", "남성", "여성", "남녀공용"
    );
    private static final Set<String> COLOR_TOKENS = Set.of(
            "BLACK", "WHITE", "BLUE", "NAVY", "GRAY", "BEIGE",
            "KHAKI", "GREEN", "RED", "PINK", "PURPLE", "BROWN",
            "YELLOW", "MUSTARD", "IVORY", "CREAM", "CHARCOAL", "ORANGE"
    );
    private static final Map<String, String> CANONICAL_TOKENS = createCanonicalTokens();

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductRecommendationsRes getRecommendations(Long productId, int limit) {
        int safeLimit = Math.max(1, limit);
        Product target = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductProfile targetProfile = createProfile(target);

        List<Product> candidates = productRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(productId))
                .toList();

        List<ProductSummaryRes> brandSimilarProducts = candidates.stream()
                .filter(candidate -> sameBrand(target, candidate))
                .map(candidate -> new ScoredProduct(candidate, scoreSameBrand(target, targetProfile, candidate)))
                .filter(scored -> scored.score() > 0)
                .sorted(scoredProductComparator())
                .limit(safeLimit)
                .map(scored -> ProductSummaryRes.from(scored.product()))
                .toList();

        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(productId);
        brandSimilarProducts.stream()
                .map(ProductSummaryRes::productId)
                .forEach(excludedIds::add);

        List<ProductSummaryRes> similarProducts = candidates.stream()
                .filter(candidate -> !excludedIds.contains(candidate.getId()))
                .map(candidate -> new ScoredProduct(candidate, scoreGenericSimilar(target, targetProfile, candidate)))
                .filter(scored -> scored.score() > 0)
                .sorted(scoredProductComparator())
                .limit(safeLimit)
                .map(scored -> ProductSummaryRes.from(scored.product()))
                .toList();

        return new ProductRecommendationsRes(brandSimilarProducts, similarProducts);
    }

    private Comparator<ScoredProduct> scoredProductComparator() {
        return (left, right) -> {
            int byScore = Integer.compare(right.score(), left.score());
            if (byScore != 0) {
                return byScore;
            }

            BigDecimal leftPrice = left.product().getPrice() == null ? BigDecimal.ZERO : left.product().getPrice();
            BigDecimal rightPrice = right.product().getPrice() == null ? BigDecimal.ZERO : right.product().getPrice();
            int byPrice = leftPrice.compareTo(rightPrice);
            if (byPrice != 0) {
                return byPrice;
            }

            return Long.compare(left.product().getId(), right.product().getId());
        };
    }

    private int scoreSameBrand(Product target, ProductProfile targetProfile, Product candidate) {
        if (!sameBrand(target, candidate)) {
            return 0;
        }

        ProductProfile candidateProfile = createProfile(candidate);
        int exactTitleBonus = targetProfile.normalizedTitle().equals(candidateProfile.normalizedTitle()) ? 500 : 0;
        int modelBonus = shareModelCode(targetProfile, candidateProfile) ? 400 : 0;
        int sameCoreBonus = targetProfile.coreTokens().equals(candidateProfile.coreTokens()) ? 250 : 0;
        int sameColorBonus = sameColors(targetProfile, candidateProfile) ? 60 : 0;
        int sameCategoryBonus = sameCategory(target, candidate) ? 80 : 0;
        int mallVariantBonus = exactTitleBonus > 0 && !sameMall(target, candidate) ? 120 : 0;
        int overlapScore = coreOverlapScore(targetProfile, candidateProfile, 70, 140);

        int total = exactTitleBonus + modelBonus + sameCoreBonus + sameColorBonus + sameCategoryBonus + mallVariantBonus + overlapScore;
        return total >= 140 ? total : 0;
    }

    private int scoreGenericSimilar(Product target, ProductProfile targetProfile, Product candidate) {
        ProductProfile candidateProfile = createProfile(candidate);
        int overlapScore = coreOverlapScore(targetProfile, candidateProfile, 60, 120);
        if (overlapScore == 0) {
            return 0;
        }

        int sameCategoryBonus = sameCategory(target, candidate) ? 70 : 0;
        int sameColorBonus = sameColors(targetProfile, candidateProfile) ? 40 : 0;
        int exactTitlePenalty = targetProfile.normalizedTitle().equals(candidateProfile.normalizedTitle()) ? 30 : 0;

        int total = overlapScore + sameCategoryBonus + sameColorBonus - exactTitlePenalty;
        return total >= 150 ? total : 0;
    }

    private int coreOverlapScore(ProductProfile left, ProductProfile right, int tokenWeight, int overlapWeight) {
        Set<String> commonCoreTokens = intersection(left.coreTokens(), right.coreTokens());
        if (commonCoreTokens.isEmpty()) {
            return 0;
        }

        double overlap = commonCoreTokens.size() / (double) Math.max(1, Math.min(left.coreTokens().size(), right.coreTokens().size()));
        double jaccard = commonCoreTokens.size() / (double) Math.max(1, unionSize(left.coreTokens(), right.coreTokens()));
        double similarity = Math.max(overlap, jaccard);

        return (commonCoreTokens.size() * tokenWeight) + (int) Math.round(similarity * overlapWeight);
    }

    private boolean sameBrand(Product left, Product right) {
        return normalizeText(left.getBrand()).equals(normalizeText(right.getBrand()));
    }

    private boolean sameCategory(Product left, Product right) {
        String leftPath = left.getCategory() == null ? null : left.getCategory().getPath();
        String rightPath = right.getCategory() == null ? null : right.getCategory().getPath();
        if (leftPath == null || rightPath == null) {
            return false;
        }
        return leftPath.equalsIgnoreCase(rightPath);
    }

    private boolean sameMall(Product left, Product right) {
        String leftMall = left.getMallName() == null ? "" : left.getMallName().trim();
        String rightMall = right.getMallName() == null ? "" : right.getMallName().trim();
        return leftMall.equalsIgnoreCase(rightMall);
    }

    private boolean sameColors(ProductProfile left, ProductProfile right) {
        if (left.colors().isEmpty() || right.colors().isEmpty()) {
            return false;
        }
        return left.colors().equals(right.colors());
    }

    private boolean shareModelCode(ProductProfile left, ProductProfile right) {
        if (left.modelCodes().isEmpty() || right.modelCodes().isEmpty()) {
            return false;
        }
        return !intersection(left.modelCodes(), right.modelCodes()).isEmpty();
    }

    private ProductProfile createProfile(Product product) {
        Set<String> brandTokens = new HashSet<>(tokenize(product.getBrand()));
        List<String> titleTokens = tokenize(product.getTitle());
        List<String> comparableTokens = new ArrayList<>();
        Set<String> colors = new HashSet<>();
        Set<String> modelCodes = new HashSet<>();

        for (String token : titleTokens) {
            if (brandTokens.contains(token) || NOISE_TOKENS.contains(token)) {
                continue;
            }
            if (isLikelyModelCode(token)) {
                modelCodes.add(token);
                continue;
            }
            comparableTokens.add(token);
            if (COLOR_TOKENS.contains(token)) {
                colors.add(token);
            }
        }

        Set<String> coreTokens = new HashSet<>(comparableTokens);
        coreTokens.removeAll(colors);

        return new ProductProfile(
                normalizeText(product.getTitle()),
                Set.copyOf(coreTokens),
                Set.copyOf(colors),
                Set.copyOf(modelCodes)
        );
    }

    private List<String> tokenize(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return List.of();
        }

        return List.of(normalized.split(" ")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(token -> CANONICAL_TOKENS.getOrDefault(token, token))
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String preprocessed = value
                .replace("후드집업", "후드 집업")
                .replace("후드 집-업", "후드 집업")
                .replace("zip-up", "zip up")
                .replace("Zip-up", "Zip up")
                .replace("L/S", "LONG SLEEVE")
                .replace("l/s", "long sleeve")
                .replace("S/S", "SHORT SLEEVE")
                .replace("s/s", "short sleeve");

        return NORMALIZE_PATTERN.matcher(preprocessed)
                .replaceAll(" ")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private boolean isLikelyModelCode(String token) {
        if (token.matches("^[A-Z]{1,3}\\d{2,}$")) {
            return true;
        }
        if (token.matches("^[A-Z]{1,4}-\\d+[A-Z0-9-]*$")) {
            return true;
        }
        if (token.matches("^\\d{3,}$")) {
            return true;
        }
        return token.length() <= 1;
    }

    private int unionSize(Set<String> left, Set<String> right) {
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return union.size();
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection;
    }

    private static Map<String, String> createCanonicalTokens() {
        Map<String, String> map = new HashMap<>();
        map.put("BLACKS", "BLACK");
        map.put("블랙", "BLACK");
        map.put("BK", "BLACK");
        map.put("WHITE", "WHITE");
        map.put("화이트", "WHITE");
        map.put("BLUE", "BLUE");
        map.put("BLUES", "BLUE");
        map.put("블루", "BLUE");
        map.put("NAVY", "NAVY");
        map.put("네이비", "NAVY");
        map.put("GRAY", "GRAY");
        map.put("GREY", "GRAY");
        map.put("그레이", "GRAY");
        map.put("BEIGE", "BEIGE");
        map.put("베이지", "BEIGE");
        map.put("KHAKI", "KHAKI");
        map.put("카키", "KHAKI");
        map.put("BROWN", "BROWN");
        map.put("브라운", "BROWN");
        map.put("YELLOW", "YELLOW");
        map.put("머스타드", "MUSTARD");
        map.put("MUSTARD", "MUSTARD");
        map.put("IVORY", "IVORY");
        map.put("아이보리", "IVORY");
        map.put("CREAM", "CREAM");
        map.put("크림", "CREAM");
        map.put("CHARCOAL", "CHARCOAL");
        map.put("차콜", "CHARCOAL");
        map.put("ORANGE", "ORANGE");
        map.put("오렌지", "ORANGE");
        map.put("INDIGO", "INDIGO");
        map.put("인디고", "INDIGO");
        map.put("DEEP", "DEEP");
        map.put("딥", "DEEP");
        map.put("FADED", "FADED");
        map.put("페이디드", "FADED");
        map.put("WASHED", "WASHED");
        map.put("워시드", "WASHED");
        map.put("TEE", "TSHIRT");
        map.put("TEE", "TSHIRT");
        map.put("티셔츠", "TSHIRT");
        map.put("티", "TSHIRT");
        map.put("TSHIRT", "TSHIRT");
        map.put("LONG", "LONG");
        map.put("SLEEVE", "SLEEVE");
        map.put("LONGSLEEVE", "LONGSLEEVE");
        map.put("LONGSLEEVE", "LONGSLEEVE");
        map.put("긴팔", "LONGSLEEVE");
        map.put("SHORTSLEEVE", "SHORTSLEEVE");
        map.put("반팔", "SHORTSLEEVE");
        map.put("HOODIE", "HOOD");
        map.put("HOOD", "HOOD");
        map.put("후드", "HOOD");
        map.put("ZIP", "ZIPUP");
        map.put("ZIPUP", "ZIPUP");
        map.put("ZIPPER", "ZIPUP");
        map.put("집업", "ZIPUP");
        map.put("DENIM", "DENIM");
        map.put("데님", "DENIM");
        map.put("JEAN", "JEANS");
        map.put("JEANS", "JEANS");
        map.put("진", "JEANS");
        map.put("청바지", "JEANS");
        map.put("PANTS", "PANTS");
        map.put("팬츠", "PANTS");
        map.put("TROUSERS", "PANTS");
        map.put("POCKET", "POCKET");
        map.put("L/S", "LONGSLEEVE");
        map.put("S/S", "SHORTSLEEVE");
        return Map.copyOf(map);
    }

    private record ProductProfile(
            String normalizedTitle,
            Set<String> coreTokens,
            Set<String> colors,
            Set<String> modelCodes
    ) {
    }

    private record ScoredProduct(
            Product product,
            int score
    ) {
    }
}
