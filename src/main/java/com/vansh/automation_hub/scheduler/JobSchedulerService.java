package com.vansh.automation_hub.scheduler;

import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.JobRepository;
import com.vansh.automation_hub.service.TelegramService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

// "Alarm manager": har job ka apna alarm lagata, badalta aur hataata hai
@Service
public class JobSchedulerService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final TaskScheduler taskScheduler;
    private final JobRepository jobRepository;
    private final TelegramService telegramService;

    // jobId -> alarm ka "remote". Isse hum alarm cancel kar sakte hain.
    // ConcurrentHashMap: API threads aur scheduler threads dono use karte hain, isliye thread-safe
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public JobSchedulerService(TaskScheduler taskScheduler,
                               JobRepository jobRepository,
                               TelegramService telegramService) {
        this.taskScheduler = taskScheduler;
        this.jobRepository = jobRepository;
        this.telegramService = telegramService;
    }

    // App poori tarah start hone ke baad (DB aur beans ready), saare active jobs schedule karo
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleAllOnStartup() {
        jobRepository.findByActiveTrue().forEach(this::schedule);
        System.out.println("Startup: " + scheduledTasks.size() + " job(s) scheduled.");
    }

    // Job ka alarm lagao. Pehle purana alarm hata dete hain, isliye ye
    // create, update, pause aur resume, sab ke liye same method kaam karta hai.
    public void schedule(Job job) {
        cancel(job.getId());

        // Paused job ka alarm nahi lagta
        if (!job.isActive()) {
            return;
        }

        String cron = job.getCronExpression();
        if (cron == null || !CronExpression.isValidExpression(cron)) {
            System.out.println("Job " + job.getId() + " skipped: invalid or missing cron.");
            return;
        }

        // Trigger = "kab chalna hai". IST dene se server kisi bhi timezone mein ho, time hamara chalega
        CronTrigger trigger = new CronTrigger(cron, IST);

        // Alarm bajne par execute(jobId) chalega
        Long jobId = job.getId();
        ScheduledFuture<?> future = taskScheduler.schedule(() -> execute(jobId), trigger);

        if (future != null) {
            scheduledTasks.put(jobId, future);
            System.out.println("Scheduled job " + jobId + " with cron '" + cron + "'");
        }
    }

    // Alarm hatao (delete, pause ya reschedule par)
    public void cancel(Long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            // false = agar abhi chal raha hai toh use beech mein mat roko
            future.cancel(false);
            System.out.println("Cancelled schedule for job " + jobId);
        }
    }

    // Alarm bajne par: job DB se FRESH uthao, taaki naya message turant lage
    private void execute(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.isActive()) {
                System.out.println("Running job: " + job.getName());
                telegramService.sendMessage(job.getMessage());
            }
        });
    }
}