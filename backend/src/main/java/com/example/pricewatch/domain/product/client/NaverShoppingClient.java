package com.example.pricewatch.domain.product.client;

import com.example.pricewatch.domain.product.client.dto.NaverShoppingSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NaverShoppingClient {

    private static final String SHOPPING_EXCLUDE_OPTIONS = "used:rental:cbshop";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ApiRequestArchiveService apiRequestArchiveService;

    @Value("${naver.shopping.base-url:https://openapi.naver.com}")
    private String baseUrl;
    @Value("${naver.shopping.client-id:}")
    private String clientId;
    @Value("${naver.shopping.client-secret:}")
    private String clientSecret;

    public NaverShoppingSearchResponse search(String query, int display) {
        return search(query, display, 1);
    }

    public NaverShoppingSearchResponse search(String query, int display, int start) {
        RestClient client = restClientBuilder.baseUrl(baseUrl).build();
        String rawResponse = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/shop.json")
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("start", start)
                        .queryParam("sort", "sim")
                        .queryParam("exclude", SHOPPING_EXCLUDE_OPTIONS)
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(String.class);

        apiRequestArchiveService.archiveNaverSearch(query, rawResponse);

        if (rawResponse == null || rawResponse.isBlank()) {
            return new NaverShoppingSearchResponse(0, start, display, java.util.List.of());
        }

        try {
            NaverShoppingSearchResponse response = objectMapper.readValue(rawResponse, NaverShoppingSearchResponse.class);
            return response == null ? new NaverShoppingSearchResponse(0, start, display, java.util.List.of()) : response;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Naver shopping response", e);
        }
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
