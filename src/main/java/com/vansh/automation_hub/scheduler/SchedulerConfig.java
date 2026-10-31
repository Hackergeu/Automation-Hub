package com.vansh.automation_hub.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    // TaskScheduler = runtime pe tasks schedule karne wala engine.
    // Pool of 5 threads: ek saath 5 jobs chal sakte hain, ek slow job baaki ko nahi rokta.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("job-scheduler-");
        // Cancel kiye gaye tasks queue se turant hat jaayein
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}