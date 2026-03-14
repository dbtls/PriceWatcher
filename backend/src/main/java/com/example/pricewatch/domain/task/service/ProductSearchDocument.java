package com.example.pricewatch.domain.task.service;

import com.example.pricewatch.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "product-search")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "product_index_analyzer", searchAnalyzer = "product_search_analyzer")
    private String brand;

    @Field(type = FieldType.Text, analyzer = "product_index_analyzer", searchAnalyzer = "product_search_analyzer")
    private String title;

    @Field(type = FieldType.Keyword)
    private String mallName;

    @Field(type = FieldType.Keyword)
    private String naverProductId;

    @Field(type = FieldType.Keyword)
    private String externalKey;

    @Field(type = FieldType.Keyword)
    private String url;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Text, analyzer = "product_index_analyzer", searchAnalyzer = "product_search_analyzer")
    private String categoryPath;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    public static ProductSearchDocument from(Product product) {
        String categoryPath = product.getCategory() == null ? null : product.getCategory().getPath();
        return ProductSearchDocument.builder()
                .id(product.getId())
                .brand(product.getBrand())
                .title(product.getTitle())
                .mallName(product.getMallName())
                .naverProductId(product.getNaverProductId())
                .externalKey(product.getExternalKey())
                .url(product.getUrl())
                .imageUrl(product.getImageUrl())
                .categoryPath(categoryPath)
                .price(product.getPrice())
                .build();
    }
}
