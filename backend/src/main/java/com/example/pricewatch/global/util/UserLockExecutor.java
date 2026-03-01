package com.example.pricewatch.global.util;

import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 기반 분산 락 실행 유틸.
 */
@Component
@RequiredArgsConstructor
public class UserLockExecutor {

    private final RedissonClient redissonClient;

    /**
     * 지정한 락 키를 획득한 상태에서 작업을 수행.
     */
    public <T> T withLock(String lockKey, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 짧은 대기 후 락을 획득하고, lease time 동안 자동 해제되도록 설정.
            boolean acquired = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ApiException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            // 현재 스레드가 락을 보유 중일 때만 안전하게 해제.
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

