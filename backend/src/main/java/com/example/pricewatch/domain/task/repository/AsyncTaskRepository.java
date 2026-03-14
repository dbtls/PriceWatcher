package com.example.pricewatch.domain.task.repository;

import com.example.pricewatch.domain.task.entity.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsyncTaskRepository extends JpaRepository<AsyncTask, Long> {
    Optional<AsyncTask> findByRequestId(String requestId);

    boolean existsByProductIdAndIsSuccess(Long productId, boolean isSuccess);

    List<AsyncTask> findTop100ByIsSuccessFalseOrderByIdAsc();
}
