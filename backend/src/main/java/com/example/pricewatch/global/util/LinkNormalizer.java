package com.example.pricewatch.global.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class LinkNormalizer {

    private LinkNormalizer() {
    }

    public static String normalize(String rawLink) {
        if (rawLink == null || rawLink.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(rawLink.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            String query = uri.getQuery();
            String normalized = scheme + "://" + host + path;
            if (query != null && !query.isBlank()) {
                normalized += "?" + query;
            }
            return normalized;
        } catch (URISyntaxException e) {
            return rawLink.trim();
        }
    }
}

