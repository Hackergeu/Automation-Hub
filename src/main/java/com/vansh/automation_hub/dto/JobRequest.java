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

        // Ab 3 channel allowed hain. Ye sirf naam ki shape check karta hai;
        // "configured hai ya nahi" ki jaanch service mein hoti hai
        @NotBlank(message = "channel is required")
        @Pattern(regexp = "TELEGRAM|DISCORD|EMAIL",
                message = "channel must be one of TELEGRAM, DISCORD, EMAIL")
        String channel,

        @NotBlank(message = "cronExpression is required")
        @Size(max = 100, message = "cronExpression is too long")
        String cronExpression,

        // Optional. Sirf EMAIL ke liye zaroori (receiver ka address)
        @Size(max = 200, message = "target must be at most 200 characters")
        String target
) {
}