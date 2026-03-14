package com.example.pricewatch.domain.price.batch;

import com.example.pricewatch.global.util.RedisKeys;
import com.example.pricewatch.global.util.UserLockExecutor;
import com.example.pricewatch.domain.price.dto.PriceRefreshBatchSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceRefreshJobRunner {

    private final JobLauncher jobLauncher;
    private final Job priceRefreshJob;
    private final UserLockExecutor userLockExecutor;

    @Value("${app.batch.price.enabled:false}")
    private boolean batchEnabled;

    @Scheduled(cron = "${app.batch.price.cron:0 0 4 * * *}")
    public void runDaily() {
        if (!batchEnabled) {
            return;
        }
        runOnce();
    }

    public PriceRefreshBatchSummary runOnce() {
        LocalDate batchDate = LocalDate.now();
        LocalDateTime startedAt = LocalDateTime.now();
        String lockKey = RedisKeys.batchPriceLock(batchDate.toString().replace("-", ""));

        return userLockExecutor.withLock(lockKey, 2, 3600, () -> {
            try {
                JobParameters parameters = new JobParametersBuilder()
                        .addString("batchDate", batchDate.toString())
                        .addString("batchStartedAt", startedAt.toString())
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();
                JobExecution jobExecution = jobLauncher.run(priceRefreshJob, parameters);
                PriceRefreshBatchSummary firstPass = findSummary(jobExecution, "firstPassSummary");
                PriceRefreshBatchSummary secondPass = findSummary(jobExecution, "secondPassSummary");
                return mergeSummaries(firstPass, secondPass);
            } catch (Exception e) {
                log.error("Failed to run price refresh job", e);
                return PriceRefreshBatchSummary.empty();
            }
        });
    }

    private PriceRefreshBatchSummary findSummary(JobExecution jobExecution, String key) {
        Object fromJobContext = jobExecution.getExecutionContext().get(key);
        if (fromJobContext instanceof PriceRefreshBatchSummary summary) {
            return summary;
        }

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Object fromStepContext = stepExecution.getExecutionContext().get(key);
            if (fromStepContext instanceof PriceRefreshBatchSummary summary) {
                return summary;
            }
        }

        return null;
    }

    private PriceRefreshBatchSummary mergeSummaries(PriceRefreshBatchSummary firstPass, PriceRefreshBatchSummary secondPass) {
        PriceRefreshBatchSummary first = firstPass == null ? PriceRefreshBatchSummary.empty() : firstPass;
        PriceRefreshBatchSummary second = secondPass == null ? PriceRefreshBatchSummary.empty() : secondPass;
        return first.merge(second);
    }
}
