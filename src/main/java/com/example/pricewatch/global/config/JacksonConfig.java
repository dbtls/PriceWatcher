package com.example.pricewatch.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule()); // LocalDateTime 직렬화 대비
        return om;
    }
}
