package com.example.pricewatch.domain.task.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchIndexService {

    private static final List<String> ALLOWED_MALLS = List.of("무신사", "29CM", "하이츠스토어", "EQL");
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+");
    private static final Set<String> WEAK_TOKENS = Set.of(
            "BLACK", "BLACKS", "블랙", "검정", "검은색",
            "WHITE", "화이트", "흰색",
            "BLUE", "BLUES", "블루", "파랑",
            "NAVY", "네이비",
            "GRAY", "GREY", "그레이", "회색",
            "BEIGE", "베이지",
            "KHAKI", "카키",
            "GREEN", "그린",
            "RED", "레드",
            "PINK", "핑크",
            "PURPLE", "퍼플",
            "BROWN", "브라운"
    );

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${app.search.elasticsearch.enabled:false}")
    private boolean elasticsearchEnabled;
    @Value("${app.search.elasticsearch.min-score:2.5}")
    private float minScore;

    @Transactional(readOnly = true)
    public List<ProductSummaryRes> search(String q, int page, int size) {
        if (!elasticsearchEnabled) {
            return null;
        }

        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(qb -> qb.bool(bool -> bool
                            .should(should -> should.matchPhrase(phrase -> phrase.field("title").query(q).boost(5.0f)))
                            .should(should -> should.matchPhrase(phrase -> phrase.field("brand").query(q).boost(4.0f)))
                            .should(should -> should.match(match -> match.field("brand").query(q).boost(3.0f)))
                            .should(should -> should.match(match -> match.field("title").query(q).boost(2.0f)))
                            .should(should -> should.match(match -> match.field("categoryPath").query(q)))
                            .minimumShouldMatch("1")
                            .filter(filter -> filter.terms(terms -> terms
                                    .field("mallName")
                                    .terms(termValues -> termValues.value(ALLOWED_MALLS.stream().map(FieldValue::of).toList()))
                            ))
                    ))
                    .withMinScore(minScore)
                    .withPageable(PageRequest.of(page, size))
                    .build();

            List<String> tokens = tokenize(q);
            SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(query, ProductSearchDocument.class);
            return hits.getSearchHits().stream()
                    .filter(hit -> passesTokenGate(tokens, hit.getContent()))
                    .map(SearchHit::getContent)
                    .map(this::mapToResponse)
                    .toList();
        } catch (Exception e) {
            log.warn("Elasticsearch search failed. Fallback to DB search. reason={}", e.toString());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public void upsertProduct(Long productId) {
        if (!elasticsearchEnabled) {
            return;
        }

        ensureIndex();

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            elasticsearchOperations.delete(String.valueOf(productId), ProductSearchDocument.class);
            return;
        }

        elasticsearchOperations.save(ProductSearchDocument.from(product));
    }

    @Transactional(readOnly = true)
    public void deleteProduct(Long productId) {
        if (!elasticsearchEnabled) {
            return;
        }
        elasticsearchOperations.delete(String.valueOf(productId), ProductSearchDocument.class);
    }

    @Transactional(readOnly = true)
    public void reindexAllProducts() {
        if (!elasticsearchEnabled) {
            return;
        }

        recreateIndex();

        List<ProductSearchDocument> documents = productRepository.findAll().stream()
                .map(ProductSearchDocument::from)
                .toList();
        for (ProductSearchDocument document : documents) {
            elasticsearchOperations.save(document);
        }
    }

    private ProductSummaryRes mapToResponse(ProductSearchDocument document) {
        return new ProductSummaryRes(
                document.getId(),
                document.getBrand(),
                document.getTitle(),
                document.getPrice(),
                document.getMallName(),
                document.getNaverProductId(),
                document.getExternalKey(),
                document.getUrl(),
                document.getImageUrl(),
                document.getCategoryPath()
        );
    }

    private void ensureIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductSearchDocument.class);
        if (indexOps.exists()) {
            return;
        }
        createIndex(indexOps);
    }

    private void recreateIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductSearchDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        createIndex(indexOps);
    }

    private void createIndex(IndexOperations indexOps) {
        Map<String, Object> settings = createSettings();
        Document mapping = indexOps.createMapping(ProductSearchDocument.class);
        indexOps.create(settings, mapping);
        indexOps.refresh();
    }

    private Map<String, Object> createSettings() {
        List<String> synonyms = loadSynonyms();

        Map<String, Object> synonymFilter = new LinkedHashMap<>();
        synonymFilter.put("type", "synonym_graph");
        synonymFilter.put("synonyms", synonyms);

        Map<String, Object> indexAnalyzer = new LinkedHashMap<>();
        indexAnalyzer.put("tokenizer", "standard");
        indexAnalyzer.put("filter", List.of("lowercase", "asciifolding"));

        Map<String, Object> searchAnalyzer = new LinkedHashMap<>();
        searchAnalyzer.put("tokenizer", "standard");
        searchAnalyzer.put("filter", List.of("lowercase", "asciifolding", "product_synonym_filter"));

        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("product_synonym_filter", synonymFilter);

        Map<String, Object> analyzer = new LinkedHashMap<>();
        analyzer.put("product_index_analyzer", indexAnalyzer);
        analyzer.put("product_search_analyzer", searchAnalyzer);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("filter", filter);
        analysis.put("analyzer", analyzer);

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("analysis", analysis);
        return settings;
    }

    private List<String> loadSynonyms() {
        try {
            ClassPathResource resource = new ClassPathResource("elasticsearch/product-search-synonyms.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .filter(line -> !line.startsWith("#"))
                        .toList();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load product search synonyms", e);
        }
    }

    private boolean passesTokenGate(List<String> tokens, ProductSearchDocument document) {
        if (tokens.isEmpty()) {
            return true;
        }

        List<String> strongTokens = tokens.stream()
                .filter(token -> !WEAK_TOKENS.contains(token))
                .toList();
        if (strongTokens.isEmpty()) {
            return true;
        }

        String normalizedBrand = normalizeText(document.getBrand());
        String normalizedTitle = normalizeText(document.getTitle());
        String normalizedCategory = normalizeText(document.getCategoryPath());

        return strongTokens.stream().anyMatch(token ->
                normalizedBrand.contains(token) ||
                        normalizedTitle.contains(token) ||
                        normalizedCategory.contains(token)
        );
    }

    private List<String> tokenize(String value) {
        return List.of(normalizeText(value).split(" ")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return NORMALIZE_PATTERN.matcher(value)
                .replaceAll(" ")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }
}
