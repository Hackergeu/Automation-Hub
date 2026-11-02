package com.vansh.automation_hub.controller;

import com.vansh.automation_hub.dto.StatsResponse;
import com.vansh.automation_hub.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    // GET /api/stats (login zaroori, /api/** ke andar hai)
    @GetMapping
    public StatsResponse stats() {
        return statsService.last24Hours();
    }
}