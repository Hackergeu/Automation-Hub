package com.vansh.automation_hub;

import com.vansh.automation_hub.model.Job;
import com.vansh.automation_hub.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final JobRepository jobRepository;

    public DataSeeder(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public void run(String... args) {
        // Sirf tab insert karo jab table khali ho, warna har restart pe duplicate banenge
        if (jobRepository.count() == 0) {
            jobRepository.save(new Job(
                    "Morning goal reminder",
                    "Good morning Vansh! Aaj ka goal: ek project commit karna 🚀",
                    "TELEGRAM",
                    "0 0 8 * * *"));   // roz subah 8 baje
            System.out.println("Sample job DB mein save ho gaya.");
        }
    }
}