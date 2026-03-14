package com.example.pricewatch.domain.product.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ApiRequestArchiveService {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    @Value("${app.debug.archive-api-responses:false}")
    private boolean archiveEnabled;

    @Value("${app.debug.api-request-dir:apiRequests}")
    private String archiveDir;

    public void archiveNaverSearch(String query, String rawResponse) {
        if (!archiveEnabled) {
            log.info("API archive skipped because archiveEnabled=false");
            return;
        }

        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("API archive skipped because rawResponse is blank for query={}", query);
            return;
        }

        try {
            Path directory = Paths.get(archiveDir);
            Files.createDirectories(directory);

            String timestamp = LocalDateTime.now().format(FILE_TIME_FORMAT);
            String safeQuery = sanitizeFilePart(query);
            String fileName = "naver-search-" + timestamp + "-" + safeQuery + ".json";
            Path target = directory.resolve(fileName).toAbsolutePath();
            Files.writeString(target, rawResponse, StandardCharsets.UTF_8);
            log.info("Archived Naver response to {}", target);
        } catch (IOException e) {
            log.error("Failed to archive Naver response into {}", Paths.get(archiveDir).toAbsolutePath(), e);
        }
    }

    private String sanitizeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "blank";
        }

        String sanitized = value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s+", "-");
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }
}
