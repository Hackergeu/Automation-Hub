package com.vansh.automation_hub.dto;

// Pichhle 24 ghante ke runs ka saar
public record StatsResponse(long runsLast24h, long successLast24h, long failedLast24h) {
}