package com.vansh.automation_hub.repository;

import com.vansh.automation_hub.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByActiveTrue();
}