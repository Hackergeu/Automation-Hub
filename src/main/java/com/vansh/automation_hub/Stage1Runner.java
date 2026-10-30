package com.vansh.automation_hub;

import com.vansh.automation_hub.service.TelegramService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// CommandLineRunner = app start hone ke turant baad ek baar chalta hai
//@Component
public class Stage1Runner implements CommandLineRunner {

    private final TelegramService telegramService;

    // Spring khud TelegramService yahan inject kar deta hai (constructor injection)
    public Stage1Runner(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public void run(String... args) {
        // Abhi trigger = "app start hua". Stage 2 mein ise time-based banayenge
        telegramService.sendMessage("Hello Vansh! This is your first automated message 🚀");
    }
}