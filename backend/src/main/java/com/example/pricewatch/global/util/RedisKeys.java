package com.example.pricewatch.global.util;

/**
 * Redis 키 네이밍 유틸.
 */
public final class RedisKeys {

    /**
     * 유틸 클래스 인스턴스화 방지 생성자.
     */
    private RedisKeys() {
    }

    /**
     * 검색 single-flight 락 키를 생성.
     */
    public static String searchLock(String queryHash) {
        return "lock:search:" + queryHash;
    }

    /**
     * 검색 결과 캐시 키를 생성.
     */
    public static String searchCache(String queryHash) {
        return "cache:search:" + queryHash;
    }

    /**
     * AI single-flight 락 키를 생성.
     */
    public static String aiLock(String promptHash) {
        return "lock:ai:" + promptHash;
    }

    /**
     * AI 결과 캐시 키를 생성.
     */
    public static String aiCache(String promptHash) {
        return "cache:ai:" + promptHash;
    }

    /**
     * 가격 배치 중복 방지 락 키를 생성.
     */
    public static String batchPriceLock(String yyyyMMdd) {
        return "lock:batch:price:" + yyyyMMdd;
    }
}

