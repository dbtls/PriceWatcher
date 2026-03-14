package com.example.pricewatch.domain.email.controller;

import com.example.pricewatch.domain.email.dto.EmailDispatchSummary;
import com.example.pricewatch.domain.email.dto.EmailOutboxStatsRes;
import com.example.pricewatch.domain.email.service.EmailOutboxService;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/email/outbox")
public class EmailOutboxController {

    private final EmailOutboxService emailOutboxService;

    @PostMapping("/dispatch")
    public ResponseEntity<ResponseDto<EmailDispatchSummary>> dispatchNow() {
        return ResponseEntity.ok(ResponseDto.success("이메일 outbox 수동 발송 성공", emailOutboxService.dispatchPendingNow()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ResponseDto<EmailOutboxStatsRes>> getStats() {
        return ResponseEntity.ok(ResponseDto.success("이메일 outbox 상태 조회 성공", emailOutboxService.getStats()));
    }
}
