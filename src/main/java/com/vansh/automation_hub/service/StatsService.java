package com.vansh.automation_hub.service;

import com.vansh.automation_hub.dto.StatsResponse;
import com.vansh.automation_hub.model.ExecutionStatus;
import com.vansh.automation_hub.repository.ExecutionLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StatsService {

    private final ExecutionLogRepository executionLogRepository;

    public StatsService(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    public StatsResponse last24Hours() {
        // Logs bhi LocalDateTime.now() se likhe gaye hain, isliye same tarah se cutoff nikalte hain
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        long total = executionLogRepository.countByStartedAtAfter(since);
        long success = executionLogRepository.countByStartedAtAfterAndStatus(since, ExecutionStatus.SUCCESS);

        // Failed = baaki sab (alag query ki zarurat nahi)
        return new StatsResponse(total, success, total - success);
    }
}