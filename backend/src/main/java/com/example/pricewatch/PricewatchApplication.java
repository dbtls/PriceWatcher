package com.example.pricewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 애플리케이션 메인 부트스트랩 클래스.
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class PricewatchApplication {

    /**
     * 애플리케이션 실행 진입점.
     */
    public static void main(String[] args) {
        SpringApplication.run(PricewatchApplication.class, args);
    }
}
