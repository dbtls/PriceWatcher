package com.example.pricewatch.domain.email.repository;

import com.example.pricewatch.domain.email.entity.EmailOutbox;
import com.example.pricewatch.domain.email.entity.EmailOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    List<EmailOutbox> findTop100ByStatusOrderByCreatedAtAsc(EmailOutboxStatus status);
}

