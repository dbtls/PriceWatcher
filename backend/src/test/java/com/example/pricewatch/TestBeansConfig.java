package com.example.pricewatch;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트 컨텍스트 전용 빈 설정.
 */
@TestConfiguration
public class TestBeansConfig {

    /**
     * 테스트용 RedissonClient 목 빈 등록.
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return Mockito.mock(RedissonClient.class);
    }
}
