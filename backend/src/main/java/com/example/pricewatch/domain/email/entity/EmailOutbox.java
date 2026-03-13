package com.example.pricewatch.domain.email.entity;

import com.example.pricewatch.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailOutboxStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 1000)
    private String lastError;

    private LocalDateTime sentAt;

    public void markSent() {
        this.status = EmailOutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = EmailOutboxStatus.FAILED;
        this.attemptCount += 1;
        this.lastError = errorMessage == null ? null : errorMessage.substring(0, Math.min(errorMessage.length(), 1000));
    }

    public void retry() {
        this.status = EmailOutboxStatus.PENDING;
    }
}

