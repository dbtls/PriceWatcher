package com.example.pricewatch.domain.ai.controller;

import com.example.pricewatch.domain.ai.dto.AiRequest;
import com.example.pricewatch.domain.ai.dto.AiResponse;
import com.example.pricewatch.domain.ai.service.AiService;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    /**
     * AI 요청 실행.
     */
    @PostMapping
    public ResponseEntity<ResponseDto<AiResponse>> ask(@RequestBody AiRequest req) {
        return ResponseEntity.ok(ResponseDto.success("AI 처리 성공", aiService.process(req)));
    }
}
