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

    // TELEGRAM, DISCORD ya EMAIL
    @Column(nullable = false)
    private String channel;

    // NAYA: channel ke hisaab se "kisko". Email ke liye address; baaki channels ke liye khali (null)
    @Column(length = 200)
    private String target;

    private String cronExpression;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    public Job() {
    }

    // Purana 4-argument constructor (DataSeeder isi ko use karta hai), target null rahega
    public Job(String name, String message, String channel, String cronExpression) {
        this(name, message, channel, cronExpression, null);
    }

    public Job(String name, String message, String channel, String cronExpression, String target) {
        this.name = name;
        this.message = message;
        this.channel = channel;
        this.cronExpression = cronExpression;
        this.target = target;
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
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}