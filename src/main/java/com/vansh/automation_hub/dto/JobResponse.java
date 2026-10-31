package com.vansh.automation_hub.dto;

import com.vansh.automation_hub.model.Job;

import java.time.LocalDateTime;

// Client ko jo data wapas milega. Entity ko seedha expose nahi karte.
public record JobResponse(
        Long id,
        String name,
        String message,
        String channel,
        boolean active,
        LocalDateTime createdAt
) {

    // Entity se response banane ka helper
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getMessage(),
                job.getChannel(),
                job.isActive(),
                job.getCreatedAt());
    }
}