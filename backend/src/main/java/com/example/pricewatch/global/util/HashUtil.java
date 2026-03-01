package com.example.pricewatch.global.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 해시 계산 유틸.
 */
public final class HashUtil {

    /**
     * 유틸 클래스 인스턴스화 방지 생성자.
     */
    private HashUtil() {
    }

    /**
     * 입력 문자열의 SHA-256 해시(16진수 문자열)를 반환.
     */
    public static String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

