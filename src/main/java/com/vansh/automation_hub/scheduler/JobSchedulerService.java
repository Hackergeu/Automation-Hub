package com.vansh.automation_hub.scheduler;

import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.JobRepository;
import com.vansh.automation_hub.service.JobExecutionService;
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

@Service
public class JobSchedulerService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final TaskScheduler taskScheduler;
    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public JobSchedulerService(TaskScheduler taskScheduler,
                               JobRepository jobRepository,
                               JobExecutionService jobExecutionService) {
        this.taskScheduler = taskScheduler;
        this.jobRepository = jobRepository;
        this.jobExecutionService = jobExecutionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleAllOnStartup() {
        jobRepository.findByActiveTrue().forEach(this::schedule);
        System.out.println("Startup: " + scheduledTasks.size() + " job(s) scheduled.");
    }

    public void schedule(Job job) {
        cancel(job.getId());

        if (!job.isActive()) {
            return;
        }

        String cron = job.getCronExpression();
        if (cron == null || !CronExpression.isValidExpression(cron)) {
            System.out.println("Job " + job.getId() + " skipped: invalid or missing cron.");
            return;
        }

        CronTrigger trigger = new CronTrigger(cron, IST);
        Long jobId = job.getId();
        ScheduledFuture<?> future = taskScheduler.schedule(() -> execute(jobId), trigger);

        if (future != null) {
            scheduledTasks.put(jobId, future);
            System.out.println("Scheduled job " + jobId + " with cron '" + cron + "'");
        }
    }

    public void cancel(Long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            System.out.println("Cancelled schedule for job " + jobId);
        }
    }

    // Alarm bajne par: job DB se fresh uthao, active hai toh retry-wale executor ko do
    private void execute(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.isActive()) {
                System.out.println("Running job: " + job.getName());
                jobExecutionService.run(job);
            }
        });
    }
}