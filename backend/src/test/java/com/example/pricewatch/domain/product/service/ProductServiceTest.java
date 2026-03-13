package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.TestBeansConfig;
import com.example.pricewatch.domain.product.dto.ProductSelectReq;
import com.example.pricewatch.domain.product.dto.ProductSelectRes;
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
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void selectCreatesNewProductByExternalIdentifiers() {
        ProductSelectReq req = new ProductSelectReq(
                null,
                "MUSINSA STANDARD",
                "Relaxed Hoodie Zip-up",
                new BigDecimal("79900"),
                "무신사",
                "123456789",
                "external-key-1",
                "https://store.musinsa.com/app/goods/123456789",
                "패션의류>남성의류>후드집업"
        );

        ProductSelectRes result = productService.select(req);

        assertThat(result.created()).isTrue();
        assertThat(result.product().productId()).isNotNull();

        Product saved = productRepository.findById(result.product().productId()).orElseThrow();
        assertThat(saved.getBrand()).isEqualTo("MUSINSA STANDARD");
        assertThat(saved.getNaverProductId()).isEqualTo("123456789");
        assertThat(saved.getExternalKey()).isEqualTo("external-key-1");
        assertThat(saved.getRefreshStatus()).isEqualTo(RefreshStatus.READY);
        assertThat(saved.getCategory()).isNotNull();
        assertThat(saved.getCategory().getPath()).isEqualTo("패션의류>남성의류>후드집업");
    }

    @Test
    void selectUpdatesExistingProductMatchedByNaverProductId() {
        Product existing = productRepository.save(
                Product.builder()
                        .brand("OLD")
                        .title("OLD TITLE")
                        .price(new BigDecimal("10000"))
                        .mallName("OLD MALL")
                        .naverProductId("999")
                        .externalKey("external-key-old")
                        .url("https://example.com/old")
                        .refreshStatus(RefreshStatus.FAILED)
                        .needsRematch(true)
                        .failCount(2)
                        .build()
        );

        ProductSelectReq req = new ProductSelectReq(
                null,
                "NEW BRAND",
                "NEW TITLE",
                new BigDecimal("55500"),
                "무신사",
                "999",
                "external-key-new",
                "https://store.musinsa.com/app/goods/999",
                "패션의류>남성의류"
        );

        ProductSelectRes result = productService.select(req);

        assertThat(result.created()).isFalse();
        assertThat(result.product().productId()).isEqualTo(existing.getId());

        Product updated = productRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getBrand()).isEqualTo("NEW BRAND");
        assertThat(updated.getTitle()).isEqualTo("NEW TITLE");
        assertThat(updated.getPrice()).isEqualByComparingTo("55500");
        assertThat(updated.getMallName()).isEqualTo("무신사");
        assertThat(updated.getExternalKey()).isEqualTo("external-key-new");
        assertThat(updated.getRefreshStatus()).isEqualTo(RefreshStatus.READY);
        assertThat(updated.isNeedsRematch()).isFalse();
    }
}
