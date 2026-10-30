package com.vansh.automation_hub.job;

import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.JobRepository;
import com.vansh.automation_hub.service.TelegramService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailyReminderJob {

    private final TelegramService telegramService;
    private final JobRepository jobRepository;

    // Spring dono cheezein khud inject kar deta hai
    public DailyReminderJob(TelegramService telegramService, JobRepository jobRepository) {
        this.telegramService = telegramService;
        this.jobRepository = jobRepository;
    }

    // Time abhi bhi properties se aata hai
    @Scheduled(cron = "${reminder.cron}", zone = "Asia/Kolkata")
    public void runActiveJobs() {
        // DB se saare active jobs lao
        List<Job> activeJobs = jobRepository.findByActiveTrue();

        // Har job ka message bhejo
        for (Job job : activeJobs) {
            System.out.println("Running job: " + job.getName());
            telegramService.sendMessage(job.getMessage());
        }
    }
}