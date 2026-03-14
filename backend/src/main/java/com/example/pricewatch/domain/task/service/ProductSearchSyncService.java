package com.example.pricewatch.domain.task.service;

import com.example.pricewatch.domain.task.dto.DbUpdateEvent;
import com.example.pricewatch.domain.task.dto.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductSearchSyncService {

    private final ApplicationEventPublisher eventPublisher;
    private final AsyncTaskTxService asyncTaskTxService;

    public void publishUpsert(Long productId) {
        publish(productId, TaskType.UPSERT);
    }

    public void publishDelete(Long productId) {
        publish(productId, TaskType.DELETE);
    }

    private void publish(Long productId, TaskType taskType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(productId, taskType);
                }
            });
            return;
        }
        publishNow(productId, taskType);
    }

    private void publishNow(Long productId, TaskType taskType) {
        String requestId = "search-sync-" + UUID.randomUUID();
        DbUpdateEvent event = new DbUpdateEvent(productId, taskType, requestId);
        asyncTaskTxService.startTask(event);
        eventPublisher.publishEvent(event);
    }
}
