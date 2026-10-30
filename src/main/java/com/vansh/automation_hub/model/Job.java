package com.vansh.automation_hub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// @Entity = ye class DB table se juda hai
// @Table = table ka naam "jobs" hoga
@Entity
@Table(name = "jobs")
public class Job {

    // Primary key. IDENTITY = MySQL khud 1, 2, 3... auto-increment karega
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Job ka naam, jaise "Morning goal reminder"
    @Column(nullable = false)
    private String name;

    // Jo message bhejna hai
    @Column(nullable = false, length = 1000)
    private String message;

    // Kis channel pe bhejna hai. Abhi sirf "TELEGRAM", Stage 7 mein aur aayenge
    @Column(nullable = false)
    private String channel;

    // true = chalega, false = paused. Delete kiye bina job band kar sakte hain
    @Column(nullable = false)
    private boolean active = true;

    // Job kab banaya gaya
    private LocalDateTime createdAt;

    // JPA ko empty constructor chahiye hota hai
    public Job() {
    }

    // Aasani ke liye: naya job banane wala constructor
    public Job(String name, String message, String channel) {
        this.name = name;
        this.message = message;
        this.channel = channel;
    }

    // DB mein save hone se theek pehle createdAt automatically set ho jayega
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters (Spring/JPA inhe use karte hain)
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}