package com.example.pricewatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 스프링 컨텍스트 구동 테스트.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
class PricewatchApplicationTests {

	/**
	 * 애플리케이션 컨텍스트 로드 검증.
	 */
	@Test
	void contextLoads() {
	}

}




