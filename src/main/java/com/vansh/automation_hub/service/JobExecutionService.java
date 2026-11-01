package com.vansh.automation_hub.service;

import com.vansh.automation_hub.exception.TelegramSendException;
import com.vansh.automation_hub.model.ExecutionLog;
import com.vansh.automation_hub.model.ExecutionStatus;
import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.ExecutionLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Ek job ko chalata hai: retry ke saath bhejta hai, aur nateeja logbook mein likhta hai
@Service
public class JobExecutionService {

    // Properties se aate hain. ":3" aur ":1000" default hain agar property na mile
    @Value("${retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    private final TelegramService telegramService;
    private final ExecutionLogRepository executionLogRepository;

    public JobExecutionService(TelegramService telegramService,
                               ExecutionLogRepository executionLogRepository) {
        this.telegramService = telegramService;
        this.executionLogRepository = executionLogRepository;
    }

    public void run(Job job) {
        LocalDateTime startedAt = LocalDateTime.now();
        int attempts = 0;
        boolean success = false;
        String error = null;
        long delay = initialDelayMs;

        while (attempts < maxAttempts) {
            attempts++;
            try {
                telegramService.sendMessage(job.getMessage());
                success = true;
                error = null;
                break; // Ho gaya, aur retry nahi chahiye

            } catch (TelegramSendException ex) {
                error = ex.getMessage();
                System.out.println("Job " + job.getId() + " attempt " + attempts
                        + "/" + maxAttempts + " failed: " + error);

                // Permanent error: retry bekaar hai, seedha ruk jao
                if (!ex.isRetryable()) {
                    break;
                }

                // Retry bacha hai toh wait karo, aur agli baar wait double karo (backoff)
                if (attempts < maxAttempts) {
                    if (!pause(delay)) {
                        error = "Interrupted while waiting to retry";
                        break;
                    }
                    delay = delay * 2;
                }
            }
        }

        // Nateeja logbook mein likho
        ExecutionLog log = new ExecutionLog(
                job.getId(),
                job.getName(),
                success ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED,
                attempts,
                error,
                startedAt,
                LocalDateTime.now());
        executionLogRepository.save(log);

        System.out.println("Job " + job.getId() + " finished: "
                + log.getStatus() + " after " + attempts + " attempt(s)");
    }

    // Thread ko kuch der sula do. false = beech mein interrupt hua (app band ho rahi hai)
    private boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            // Interrupt ka flag wapas set karna achhi practice hai
            Thread.currentThread().interrupt();
            return false;
        }
    }
}