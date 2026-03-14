package com.example.pricewatch.domain.task.service;

import com.example.pricewatch.domain.task.dto.DbUpdateEvent;
import com.example.pricewatch.domain.task.entity.AsyncTask;
import com.example.pricewatch.domain.task.repository.AsyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingSyncScheduler {

    private final AsyncTaskRepository asyncTaskRepository;
    private final DbEventListener dbEventListener;

    @Value("${app.search.elasticsearch.enabled:false}")
    private boolean elasticsearchEnabled;

    @Scheduled(fixedDelayString = "${app.search.elasticsearch.sync-fixed-delay-ms:300000}")
    public void reconcilePendingProducts() {
        if (!elasticsearchEnabled) {
            return;
        }

        var tasks = asyncTaskRepository.findTop100ByIsSuccessFalseOrderByIdAsc();
        if (tasks.isEmpty()) {
            return;
        }

        for (AsyncTask task : tasks) {
            try {
                dbEventListener.handleProductSearchUpdate(
                        new DbUpdateEvent(task.getProductId(), task.getTaskType(), task.getRequestId())
                );
            } catch (Exception e) {
                log.error("Pending product search sync 재시도 실패. requestId={}, reason={}", task.getRequestId(), e.toString());
            }
        }
    }
}
