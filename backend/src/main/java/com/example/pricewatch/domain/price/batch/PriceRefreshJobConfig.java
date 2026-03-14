package com.example.pricewatch.domain.price.batch;

import com.example.pricewatch.domain.price.service.PriceRefreshBatchService;
import com.example.pricewatch.domain.price.dto.PriceRefreshBatchSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    public Step firstPassRefreshStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PriceRefreshBatchService priceRefreshBatchService
    ) {
        return new StepBuilder("firstPassRefreshStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String batchDate = contribution.getStepExecution().getJobParameters().getString("batchDate");
                    String batchStartedAt = contribution.getStepExecution().getJobParameters().getString("batchStartedAt");
                    PriceRefreshBatchSummary summary = priceRefreshBatchService.runFirstPass(LocalDate.parse(batchDate), LocalDateTime.parse(batchStartedAt));
                    chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("firstPassSummary", summary);
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 2차 배치 Step 구성.
     */
    @Bean
    public Step secondPassRefreshStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PriceRefreshBatchService priceRefreshBatchService
    ) {
        return new StepBuilder("secondPassRefreshStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String batchDate = contribution.getStepExecution().getJobParameters().getString("batchDate");
                    String batchStartedAt = contribution.getStepExecution().getJobParameters().getString("batchStartedAt");
                    PriceRefreshBatchSummary summary = priceRefreshBatchService.runSecondPass(LocalDate.parse(batchDate), LocalDateTime.parse(batchStartedAt));
                    chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("secondPassSummary", summary);
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
