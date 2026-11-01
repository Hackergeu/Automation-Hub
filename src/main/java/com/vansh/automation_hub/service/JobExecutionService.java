package com.vansh.automation_hub.service;

import com.vansh.automation_hub.channel.ChannelRegistry;
import com.vansh.automation_hub.channel.MessageChannel;
import com.vansh.automation_hub.exception.ChannelSendException;
import com.vansh.automation_hub.model.ExecutionLog;
import com.vansh.automation_hub.model.ExecutionStatus;
import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.ExecutionLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class JobExecutionService {

    @Value("${retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    private final ChannelRegistry channelRegistry;
    private final ExecutionLogRepository executionLogRepository;

    public JobExecutionService(ChannelRegistry channelRegistry,
                               ExecutionLogRepository executionLogRepository) {
        this.channelRegistry = channelRegistry;
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
                // Dispatcher se job ke channel ka delivery boy lo, aur message bhejo.
                // Yahan koi "if TELEGRAM... else if DISCORD..." nahi hai, yahi Strategy pattern ka faayda hai
                MessageChannel channel = channelRegistry.get(job.getChannel());
                channel.send(job.getTarget(), job.getMessage());
                success = true;
                error = null;
                break;

            } catch (ChannelSendException ex) {
                error = ex.getMessage();
                System.out.println("Job " + job.getId() + " [" + job.getChannel() + "] attempt "
                        + attempts + "/" + maxAttempts + " failed: " + error);

                // Permanent error: retry bekaar hai
                if (!ex.isRetryable()) {
                    break;
                }

                // Retry bacha hai toh wait karo, aur agli baar wait double (backoff)
                if (attempts < maxAttempts) {
                    if (!pause(delay)) {
                        error = "Interrupted while waiting to retry";
                        break;
                    }
                    delay = delay * 2;
                }
            }
        }

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

    private boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}