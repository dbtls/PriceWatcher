package com.example.pricewatch.domain.task.service;

import com.example.pricewatch.domain.task.dto.DbUpdateEvent;
import com.example.pricewatch.domain.task.entity.AsyncTask;
import com.example.pricewatch.domain.task.repository.AsyncTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsyncTaskTxService {

    private final AsyncTaskRepository asyncTaskRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AsyncTask startTask(DbUpdateEvent event) {
        return asyncTaskRepository.findByRequestId(event.requestId())
                .orElseGet(() -> asyncTaskRepository.save(new AsyncTask(event)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String requestId) {
        AsyncTask task = asyncTaskRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("AsyncTask not found. requestId=" + requestId));
        task.taskSuccess();
    }
}
