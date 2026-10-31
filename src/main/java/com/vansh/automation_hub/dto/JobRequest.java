package com.vansh.automation_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JobRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "message is required")
        @Size(max = 1000, message = "message must be at most 1000 characters")
        String message,

        @NotBlank(message = "channel is required")
        @Pattern(regexp = "TELEGRAM", message = "channel must be TELEGRAM")
        String channel,

        // NAYA: cron ki sahi format ki jaanch service mein hoti hai (CronExpression.isValid)
        @NotBlank(message = "cronExpression is required")
        @Size(max = 100, message = "cronExpression is too long")
        String cronExpression
) {
}