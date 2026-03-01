package com.example.pricewatch.domain.price.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 가격 갱신 배치 Job 설정.
 */
@Configuration
@RequiredArgsConstructor
public class PriceRefreshJobConfig {

    /**
     * 가격 갱신 Job 구성.
     */
    @Bean
    public Job priceRefreshJob(JobRepository jobRepository, Step firstPassRefreshStep, Step secondPassRefreshStep) {
        return new JobBuilder("priceRefreshJob", jobRepository)
                .start(firstPassRefreshStep)
                .next(secondPassRefreshStep)
                .build();
    }

    /**
     * 1차 배치 Step 구성.
     */
    @Bean
    public Step firstPassRefreshStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("firstPassRefreshStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // TODO 브랜드 단위 1차 갱신 로직 구현.
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 2차 배치 Step 구성.
     */
    @Bean
    public Step secondPassRefreshStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("secondPassRefreshStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // TODO 미갱신 상품 2차 개별 갱신 로직 구현.
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
