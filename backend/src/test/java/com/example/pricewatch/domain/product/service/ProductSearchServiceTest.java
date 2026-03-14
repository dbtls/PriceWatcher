package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.TestBeansConfig;
import com.example.pricewatch.domain.product.dto.ProductSearchRes;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.entity.RefreshStatus;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
@Transactional
class ProductSearchServiceTest {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void searchDbFindsNormalizedAndTokenizedMatches() {
        productRepository.save(product("디스이즈네버댓", "[디스이즈네버댓] Regular Jeans Blue", "29CM"));
        productRepository.save(product("디스이즈네버댓", "[디스이즈네버댓] Denim Carpenter Short Blacks", "29CM"));
        productRepository.save(product("디스이즈네버댓", "[디스이즈네버댓] Washed Denim Short Washed Blue", "29CM"));
        productRepository.save(product("디스이즈네버댓", "[디스이즈네버댓] GD Lightning Relaxed Jeans Black", "29CM"));

        ProductSearchRes result = productSearchService.searchDb("디스이즈네버댓 Regular Jeans Blue", 0, 20);

        assertThat(result.internalResults()).isNotEmpty();
        assertThat(result.internalResults().get(0).title()).contains("Regular Jeans Blue");
        assertThat(result.totalCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchDbGuaranteesAtLeastFiveResultsWhenAvailable() {
        for (int i = 0; i < 6; i++) {
            productRepository.save(product("브랜드", "브랜드 데님 팬츠 " + i, "무신사"));
        }

        ProductSearchRes result = productSearchService.searchDb("브랜드 데님", 0, 20);

        assertThat(result.internalResults()).isNotEmpty();
        assertThat(result.totalCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchDbReturnsProductsFromDifferentMallsSeparately() {
        productRepository.save(product("파르티멘토", "[파르티멘토] VTG 페이디드 후드 집업 블랙 PY-2692 BK", "무신사"));
        productRepository.save(product("파르티멘토", "[파르티멘토] VTG Faded Hood Zip-up Black", "29CM"));

        ProductSearchRes result = productSearchService.searchDb("파르티멘토 VTG Faded Hood Zip-up Black", 0, 20);

        assertThat(result.internalResults()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.internalResults())
                .extracting(resultItem -> resultItem.mallName())
                .contains("무신사", "29CM");
    }

    @Test
    void searchDbDoesNotGroupProductsThatOnlyShareColor() {
        productRepository.save(product("파르티멘토", "[파르티멘토] VTG 페이디드 후드 집업 블랙", "무신사"));
        productRepository.save(product("파르티멘토", "[파르티멘토] Denim Carpenter Short Blacks", "29CM"));

        ProductSearchRes result = productSearchService.searchDb("파르티멘토 블랙", 0, 20);

        assertThat(result.internalResults()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void searchDbDoesNotGroupSameTitleWhenOnlyColorDiffers() {
        productRepository.save(product("커버낫", "[커버낫] Essential Waffle Knit Black", "무신사"));
        productRepository.save(product("커버낫", "[커버낫] Essential Waffle Knit Navy", "29CM"));

        ProductSearchRes result = productSearchService.searchDb("커버낫 Essential Waffle Knit", 0, 20);

        assertThat(result.internalResults()).hasSizeGreaterThanOrEqualTo(2);
    }

    private Product product(String brand, String title, String mallName) {
        return Product.builder()
                .brand(brand)
                .title(title)
                .price(new BigDecimal("10000"))
                .mallName(mallName)
                .externalKey(title + "-" + mallName)
                .url("https://example.com/" + title.hashCode())
                .refreshStatus(RefreshStatus.READY)
                .needsRematch(false)
                .failCount(0)
                .build();
    }
}
