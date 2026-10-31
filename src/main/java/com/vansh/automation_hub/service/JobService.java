package com.vansh.automation_hub.service;

import com.vansh.automation_hub.dto.JobRequest;
import com.vansh.automation_hub.dto.JobResponse;
import com.vansh.automation_hub.exception.InvalidCronException;
import com.vansh.automation_hub.exception.JobNotFoundException;
import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.JobRepository;
import com.vansh.automation_hub.scheduler.JobSchedulerService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.util.List;



@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobSchedulerService jobSchedulerService;

    public JobService(JobRepository jobRepository, JobSchedulerService jobSchedulerService) {
        this.jobRepository = jobRepository;
        this.jobSchedulerService = jobSchedulerService;
    }

    public JobResponse create(JobRequest request) {
        validateCron(request.cronExpression());

        Job job = new Job(request.name(), request.message(),
                request.channel(), request.cronExpression());
        Job saved = jobRepository.save(job);

        // DB mein save hone ke BAAD alarm lagao (id tabhi milti hai)
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

        // Cron badla ho sakta hai, isliye alarm dobara lagao (purana khud hat jaata hai)
        jobSchedulerService.schedule(saved);
        return JobResponse.from(saved);
    }

    public void delete(Long id) {
        Job job = findOrThrow(id);
        // Pehle alarm hatao, warna deleted job ka alarm bajta rahega
        jobSchedulerService.cancel(id);
        jobRepository.delete(job);
    }

    // Pause: active=false (schedule() alarm hata deta hai). Resume: active=true (alarm lagta hai)
    public JobResponse setActive(Long id, boolean active) {
        Job job = findOrThrow(id);
        job.setActive(active);
        Job saved = jobRepository.save(job);
        jobSchedulerService.schedule(saved);
        return JobResponse.from(saved);
    }

    // Galat cron app ko todega nahi, bas 400 error milega
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