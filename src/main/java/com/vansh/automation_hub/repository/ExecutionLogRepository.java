package com.vansh.automation_hub.repository;

import com.vansh.automation_hub.model.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    // Naam se query: "Top 50, is jobId ke, startedAt ke hisaab se naya pehle"
    List<ExecutionLog> findTop50ByJobIdOrderByStartedAtDesc(Long jobId);

    // Job delete hone par uske logs bhi hata do
    void deleteByJobId(Long jobId);
}