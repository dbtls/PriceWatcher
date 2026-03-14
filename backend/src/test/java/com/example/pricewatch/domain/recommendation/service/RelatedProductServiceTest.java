package com.example.pricewatch.domain.recommendation.service;

import com.example.pricewatch.TestBeansConfig;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.entity.RefreshStatus;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.recommendation.dto.ProductRecommendationsRes;
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
class RelatedProductServiceTest {

    @Autowired
    private RelatedProductService relatedProductService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void sameBrandRecommendationsPrioritizeClosestVariants() {
        Product target = productRepository.save(product("커버낫", "Selvedge Denim Pants Indigo", "무신사"));
        productRepository.save(product("커버낫", "Selvedge Denim Pants Indigo", "29CM"));
        productRepository.save(product("커버낫", "Selvedge Denim Pants Deep Indigo", "무신사"));
        productRepository.save(product("커버낫", "Heavyweight Pocket Tee Mustard", "무신사"));

        ProductRecommendationsRes result = relatedProductService.getRecommendations(target.getId(), 3);

        assertThat(result.brandSimilarProducts()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.brandSimilarProducts().get(0).title()).contains("Selvedge Denim Pants Indigo");
        assertThat(result.brandSimilarProducts().get(1).title()).contains("Deep Indigo");
    }

    @Test
    void genericRecommendationsReturnSimilarProductsAcrossBrands() {
        Product target = productRepository.save(product("커버낫", "Heavyweight Pocket Tee Mustard", "무신사"));
        productRepository.save(product("드로우핏", "Heavyweight Pocket Tee Mustard", "29CM"));
        productRepository.save(product("세터", "Heavyweight Pocket Long Sleeve Mustard", "무신사"));
        productRepository.save(product("프리즘웍스", "Selvedge Denim Pants Indigo", "무신사"));

        ProductRecommendationsRes result = relatedProductService.getRecommendations(target.getId(), 5);

        assertThat(result.similarProducts()).isNotEmpty();
        assertThat(result.similarProducts().get(0).brand()).isIn("드로우핏", "세터");
        assertThat(result.similarProducts())
                .extracting(product -> product.title().toUpperCase())
                .noneMatch(title -> title.contains("SELVEDGE DENIM PANTS"));
    }

    private Product product(String brand, String title, String mallName) {
        return Product.builder()
                .brand(brand)
                .title(title)
                .price(new BigDecimal("10000"))
                .mallName(mallName)
                .externalKey(title + "-" + mallName)
                .url("https://example.com/" + Math.abs((title + mallName).hashCode()))
                .refreshStatus(RefreshStatus.READY)
                .needsRematch(false)
                .failCount(0)
                .build();
    }
}
