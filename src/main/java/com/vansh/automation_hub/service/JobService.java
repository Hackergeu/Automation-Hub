package com.vansh.automation_hub.service;

import com.vansh.automation_hub.dto.ExecutionLogResponse;
import com.vansh.automation_hub.dto.JobRequest;
import com.vansh.automation_hub.dto.JobResponse;
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

    public JobService(JobRepository jobRepository,
                      JobSchedulerService jobSchedulerService,
                      ExecutionLogRepository executionLogRepository) {
        this.jobRepository = jobRepository;
        this.jobSchedulerService = jobSchedulerService;
        this.executionLogRepository = executionLogRepository;
    }

    public JobResponse create(JobRequest request) {
        validateCron(request.cronExpression());

        Job job = new Job(request.name(), request.message(),
                request.channel(), request.cronExpression());
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

        Job job = findOrThrow(id);
        job.setName(request.name());
        job.setMessage(request.message());
        job.setChannel(request.channel());
        job.setCronExpression(request.cronExpression());
        Job saved = jobRepository.save(job);

        jobSchedulerService.schedule(saved);
        return JobResponse.from(saved);
    }

    // @Transactional: deleteByJobId jaisi derived delete query ko transaction chahiye hoti hai
    @Transactional
    public void delete(Long id) {
        Job job = findOrThrow(id);
        jobSchedulerService.cancel(id);
        // Job ke saath uske purane logs bhi hata do, warna anaath rows bachi rahengi
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

    // NAYA: ek job ke latest 50 runs. Pehle check karo ki job exist karta hai (nahi toh 404)
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

    private Job findOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}