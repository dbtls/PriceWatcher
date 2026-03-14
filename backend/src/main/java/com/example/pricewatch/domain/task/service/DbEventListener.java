package com.example.pricewatch.domain.task.service;

import com.example.pricewatch.domain.task.dto.DbUpdateEvent;
import com.example.pricewatch.domain.task.dto.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbEventListener {

    private final ProductSearchIndexService productSearchIndexService;
    private final AsyncTaskTxService asyncTaskTxService;

    @Async("DBTaskExcutor")
    @EventListener
    @Transactional
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleProductSearchUpdate(DbUpdateEvent event) {
        log.info("비동기 ES 업데이트 시작 - 상품: {}", event.productId());

        if (event.taskType() == TaskType.DELETE) {
            productSearchIndexService.deleteProduct(event.productId());
        } else {
            productSearchIndexService.upsertProduct(event.productId());
        }

        registerAfterCommit(event);
    }

    private void registerAfterCommit(DbUpdateEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Transaction synchronization is not active. Fallback to immediate execution.");
            markSuccess(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                markSuccess(event);
            }
        });
    }

    private void markSuccess(DbUpdateEvent event) {
        asyncTaskTxService.markSuccess(event.requestId());
    }

    @Recover
    public void recover(Exception e, DbUpdateEvent event) {
        log.error("최종 ES 업데이트 실패! 직접 확인 필요 - 상품ID: {}", event.productId(), e);
    }
}
