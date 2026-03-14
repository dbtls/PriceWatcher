package com.example.pricewatch.domain.task.service;

import com.example.pricewatch.domain.task.repository.AsyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final AsyncTaskRepository taskRepository;

    @Transactional(readOnly = true)
    public boolean checkTasks(Long productId) {
        boolean isDbClean = !taskRepository.existsByProductIdAndIsSuccess(productId, false);
        return isDbClean;
    }
}
