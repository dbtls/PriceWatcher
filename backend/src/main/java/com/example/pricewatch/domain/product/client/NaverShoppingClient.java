package com.example.pricewatch.domain.product.client;

import com.example.pricewatch.domain.product.client.dto.NaverShoppingSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NaverShoppingClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${naver.shopping.base-url:https://openapi.naver.com}")
    private String baseUrl;
    @Value("${naver.shopping.client-id:}")
    private String clientId;
    @Value("${naver.shopping.client-secret:}")
    private String clientSecret;

    public NaverShoppingSearchResponse search(String query, int display) {
        RestClient client = restClientBuilder.baseUrl(baseUrl).build();
        NaverShoppingSearchResponse response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/shop.json")
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("start", 1)
                        .queryParam("sort", "sim")
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(NaverShoppingSearchResponse.class);
        return response == null ? new NaverShoppingSearchResponse(0, 1, display, java.util.List.of()) : response;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}

