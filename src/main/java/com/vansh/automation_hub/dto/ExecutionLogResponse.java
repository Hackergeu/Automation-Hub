package com.vansh.automation_hub.dto;

import com.vansh.automation_hub.model.ExecutionLog;
import com.vansh.automation_hub.model.ExecutionStatus;

import java.time.LocalDateTime;

public record ExecutionLogResponse(
        Long id,
        Long jobId,
        String jobName,
        ExecutionStatus status,
        int attempts,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static ExecutionLogResponse from(ExecutionLog log) {
        return new ExecutionLogResponse(
                log.getId(),
                log.getJobId(),
                log.getJobName(),
                log.getStatus(),
                log.getAttempts(),
                log.getErrorMessage(),
                log.getStartedAt(),
                log.getFinishedAt());
    }
}