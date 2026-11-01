package com.vansh.automation_hub.service;

import com.vansh.automation_hub.channel.ChannelRegistry;
import com.vansh.automation_hub.dto.ExecutionLogResponse;
import com.vansh.automation_hub.dto.JobRequest;
import com.vansh.automation_hub.dto.JobResponse;
import com.vansh.automation_hub.exception.InvalidChannelException;
import com.vansh.automation_hub.exception.InvalidCronException;
import com.vansh.automation_hub.exception.JobNotFoundException;
import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.ExecutionLogRepository;
import com.vansh.automation_hub.repository.JobRepository;
import com.vansh.automation_hub.scheduler.JobSchedulerService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobSchedulerService jobSchedulerService;
    private final ExecutionLogRepository executionLogRepository;
    private final ChannelRegistry channelRegistry;

    public JobService(JobRepository jobRepository,
                      JobSchedulerService jobSchedulerService,
                      ExecutionLogRepository executionLogRepository,
                      ChannelRegistry channelRegistry) {
        this.jobRepository = jobRepository;
        this.jobSchedulerService = jobSchedulerService;
        this.executionLogRepository = executionLogRepository;
        this.channelRegistry = channelRegistry;
    }

    public JobResponse create(JobRequest request) {
        validateCron(request.cronExpression());
        validateChannel(request.channel(), request.target());

        Job job = new Job(request.name(), request.message(),
                request.channel(), request.cronExpression(), request.target());
        Job saved = jobRepository.save(job);

        jobSchedulerService.schedule(saved);
        return JobResponse.from(saved);
    }

    public List<JobResponse> getAll() {
        return jobRepository.findAll().stream().map(JobResponse::from).toList();
    }

    public JobResponse getById(Long id) {
        return JobResponse.from(findOrThrow(id));
    }

    public JobResponse update(Long id, JobRequest request) {
        validateCron(request.cronExpression());
        validateChannel(request.channel(), request.target());

        Job job = findOrThrow(id);
        job.setName(request.name());
        job.setMessage(request.message());
        job.setChannel(request.channel());
        job.setTarget(request.target());
        job.setCronExpression(request.cronExpression());
        Job saved = jobRepository.save(job);

        jobSchedulerService.schedule(saved);
        return JobResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Job job = findOrThrow(id);
        jobSchedulerService.cancel(id);
        executionLogRepository.deleteByJobId(id);
        jobRepository.delete(job);
    }

    public JobResponse setActive(Long id, boolean active) {
        Job job = findOrThrow(id);
        job.setActive(active);
        Job saved = jobRepository.save(job);
        jobSchedulerService.schedule(saved);
        return JobResponse.from(saved);
    }

    public List<ExecutionLogResponse> getLogs(Long id) {
        findOrThrow(id);
        return executionLogRepository.findTop50ByJobIdOrderByStartedAtDesc(id).stream()
                .map(ExecutionLogResponse::from)
                .toList();
    }

    private void validateCron(String cron) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new InvalidCronException(cron);
        }
    }

    // NAYA: job banate waqt hi pakdo ki channel configured hai, warna baad mein chupchaap fail hota
    private void validateChannel(String channel, String target) {
        if (!channelRegistry.isAvailable(channel)) {
            throw new InvalidChannelException("channel",
                    channel + " is not configured on this server");
        }
        // EMAIL ke liye receiver ka address zaroori hai (simple shape check)
        if ("EMAIL".equals(channel)
                && (target == null || !target.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            throw new InvalidChannelException("target",
                    "a valid email address is required for EMAIL jobs");
        }
    }

    private Job findOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}