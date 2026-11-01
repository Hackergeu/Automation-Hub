package com.vansh.automation_hub.controller;

import com.vansh.automation_hub.dto.JobRequest;
import com.vansh.automation_hub.dto.JobResponse;
import com.vansh.automation_hub.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.vansh.automation_hub.dto.ExecutionLogResponse;

import java.util.List;

// @RestController = ye class HTTP requests leti hai aur JSON wapas deti hai
// @RequestMapping = saare URLs /api/jobs se shuru honge
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // POST /api/jobs -> 201 Created
    // @Valid = JobRequest ke rules (NotBlank, Size...) check karo
    // @RequestBody = JSON ko JobRequest object mein badlo
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@Valid @RequestBody JobRequest request) {
        return jobService.create(request);
    }

    // GET /api/jobs
    @GetMapping
    public List<JobResponse> getAll() {
        return jobService.getAll();
    }

    // GET /api/jobs/5  (@PathVariable = URL ka {id} hissa)
    @GetMapping("/{id}")
    public JobResponse getById(@PathVariable Long id) {
        return jobService.getById(id);
    }

    // PUT /api/jobs/5
    @PutMapping("/{id}")
    public JobResponse update(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return jobService.update(id, request);
    }

    // DELETE /api/jobs/5 -> 204 No Content (kuch wapas nahi aata)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        jobService.delete(id);
    }

    // PATCH /api/jobs/5/pause
    @PatchMapping("/{id}/pause")
    public JobResponse pause(@PathVariable Long id) {
        return jobService.setActive(id, false);
    }

    // PATCH /api/jobs/5/resume
    @PatchMapping("/{id}/resume")
    public JobResponse resume(@PathVariable Long id) {
        return jobService.setActive(id, true);
    }

    // GET /api/jobs/5/logs -> us job ke latest 50 runs (naya pehle)
    @GetMapping("/{id}/logs")
    public List<ExecutionLogResponse> getLogs(@PathVariable Long id) {
        return jobService.getLogs(id);
    }
}