package com.vansh.automation_hub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Har run ka ek record = logbook ki ek line
@Entity
@Table(name = "execution_logs")
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kis job ka run tha. Seedhi relation nahi, sirf id rakhi hai (simple aur loosely coupled)
    @Column(nullable = false)
    private Long jobId;

    // Job ka naam us waqt ka (taaki log padhne par samajh aaye)
    private String jobName;

    // EnumType.STRING = DB mein "SUCCESS"/"FAILED" text ke roop mein, number nahi
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    // Kitni koshish ki gayi
    private int attempts;

    // Fail hone par wajah (success par null)
    @Column(length = 500)
    private String errorMessage;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public ExecutionLog() {
    }

    public ExecutionLog(Long jobId, String jobName, ExecutionStatus status, int attempts,
                        String errorMessage, LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.status = status;
        this.attempts = attempts;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public String getJobName() { return jobName; }
    public ExecutionStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}