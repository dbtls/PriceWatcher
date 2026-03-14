package com.example.pricewatch.domain.task.entity;

import com.example.pricewatch.domain.task.dto.DbUpdateEvent;
import com.example.pricewatch.domain.task.dto.TaskType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestId;

    @Column(nullable = false)
    private boolean isSuccess;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskType taskType;

    @Builder
    public AsyncTask(DbUpdateEvent event) {
        this.requestId = event.requestId();
        this.productId = event.productId();
        this.taskType = event.taskType();
        this.isSuccess = false;
    }

    public void taskSuccess() {
        this.isSuccess = true;
    }
}
