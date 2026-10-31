package com.vansh.automation_hub.service;

import com.vansh.automation_hub.dto.JobRequest;
import com.vansh.automation_hub.dto.JobResponse;
import com.vansh.automation_hub.exception.JobNotFoundException;
import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Naya job banao
    public JobResponse create(JobRequest request) {
        Job job = new Job(request.name(), request.message(), request.channel());
        return JobResponse.from(jobRepository.save(job));
    }

    // Saare jobs
    public List<JobResponse> getAll() {
        return jobRepository.findAll().stream()
                .map(JobResponse::from)
                .toList();
    }

    // Ek job
    public JobResponse getById(Long id) {
        return JobResponse.from(findOrThrow(id));
    }

    // Job edit karo (name, message, channel badal sakte hain)
    public JobResponse update(Long id, JobRequest request) {
        Job job = findOrThrow(id);
        job.setName(request.name());
        job.setMessage(request.message());
        job.setChannel(request.channel());
        return JobResponse.from(jobRepository.save(job));
    }

    // Job delete karo
    public void delete(Long id) {
        Job job = findOrThrow(id);
        jobRepository.delete(job);
    }

    // Pause (false) ya resume (true). Row delete kiye bina job band/chalu hota hai
    public JobResponse setActive(Long id, boolean active) {
        Job job = findOrThrow(id);
        job.setActive(active);
        return JobResponse.from(jobRepository.save(job));
    }

    // Repeat hone wala code ek jagah: id se job lao, nahi mila toh 404 wala exception
    private Job findOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}