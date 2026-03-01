package com.example.pricewatch.domain.ai.service;

import com.example.pricewatch.domain.ai.dto.AiRequest;
import com.example.pricewatch.domain.ai.dto.AiResponse;
import com.example.pricewatch.global.util.HashUtil;
import com.example.pricewatch.global.util.RedisKeys;
import com.example.pricewatch.global.util.UserLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 처리 서비스.
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final UserLockExecutor userLockExecutor;

    /**
     * AI 요청 처리.
     */
    public AiResponse process(AiRequest req) {
        // 요청 내용을 해시해 single-flight 락 키로 사용.
        String promptHash = HashUtil.sha256((req.intent() == null ? "AUTO" : req.intent()) + ":" + req.message());
        return userLockExecutor.withLock(RedisKeys.aiLock(promptHash), () -> {
            // TODO OpenAI function-calling 연동 및 결과 검증 구현.
            String intent = req.intent() == null ? "AUTO" : req.intent();
            return AiResponse.empty(intent);
        });
    }
}
