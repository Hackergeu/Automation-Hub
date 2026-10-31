package com.vansh.automation_hub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String channel;

    // NAYA: is job ka apna schedule (Spring cron, 6 fields).
    // Nullable rakha hai taaki purani rows (jinme cron nahi tha) table update par na tootein.
    private String cronExpression;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    public Job() {
    }

    // Constructor mein ab cron bhi aata hai
    public Job(String name, String message, String channel, String cronExpression) {
        this.name = name;
        this.message = message;
        this.channel = channel;
        this.cronExpression = cronExpression;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}