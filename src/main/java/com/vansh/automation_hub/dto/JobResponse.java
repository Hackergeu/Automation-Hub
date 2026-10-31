package com.vansh.automation_hub.dto;

import com.vansh.automation_hub.model.Job;

import java.time.LocalDateTime;

public record JobResponse(
        Long id,
        String name,
        String message,
        String channel,
        String cronExpression,
        boolean active,
        LocalDateTime createdAt
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getMessage(),
                job.getChannel(),
                job.getCronExpression(),
                job.isActive(),
                job.getCreatedAt());
    }
}