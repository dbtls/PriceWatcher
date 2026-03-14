package com.example.pricewatch.global.util;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String searchLock(String queryHash) {
        return "lock:search:" + queryHash;
    }

    public static String searchCache(String queryHash) {
        return "cache:search:" + queryHash;
    }

    public static String naverDailyQuota(String yyyyMMdd) {
        return "quota:naver:daily:" + yyyyMMdd;
    }

    public static String batchPriceLock(String yyyyMMdd) {
        return "lock:batch:price:" + yyyyMMdd;
    }
}

