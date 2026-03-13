package com.example.pricewatch.domain.email.service;

import com.example.pricewatch.domain.email.entity.EmailOutbox;
import com.example.pricewatch.domain.email.entity.EmailOutboxStatus;
import com.example.pricewatch.domain.email.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final JavaMailSender mailSender;

    @Value("${app.email.dispatch-enabled:false}")
    private boolean dispatchEnabled;
    @Value("${app.email.max-attempt:3}")
    private int maxAttempt;

    @Transactional
    public void enqueue(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        emailOutboxRepository.save(
                EmailOutbox.builder()
                        .toEmail(toEmail.trim())
                        .subject(subject == null ? "" : subject)
                        .body(body == null ? "" : body)
                        .status(EmailOutboxStatus.PENDING)
                        .attemptCount(0)
                        .build()
        );
    }

    @Scheduled(fixedDelayString = "${app.email.dispatch-delay-ms:10000}")
    @Transactional
    public void dispatchPending() {
        if (!dispatchEnabled) {
            return;
        }

        List<EmailOutbox> pending = emailOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(EmailOutboxStatus.PENDING);
        for (EmailOutbox outbox : pending) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(outbox.getToEmail());
                message.setSubject(outbox.getSubject());
                message.setText(outbox.getBody());
                mailSender.send(message);
                outbox.markSent();
            } catch (Exception e) {
                outbox.markFailed(e.getMessage());
                if (outbox.getAttemptCount() < maxAttempt) {
                    outbox.retry();
                }
            }
        }
    }
}

