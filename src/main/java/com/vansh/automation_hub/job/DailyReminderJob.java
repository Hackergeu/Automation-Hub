package com.vansh.automation_hub.job;

import com.vansh.automation_hub.service.TelegramService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DailyReminderJob {

    private final TelegramService telegramService;

    // Spring TelegramService khud inject kar deta hai (jaise Stage 1 mein)
    public DailyReminderJob(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    // Trigger = time. Cron value application.properties se aati hai.
    // zone = Indian time, taaki server kahin bhi ho, time hamara hi rahe.
    // Scheduled method void hona chahiye aur uska koi parameter nahi hota.
    @Scheduled(cron = "${reminder.cron}", zone = "Asia/Kolkata")
    public void sendReminder() {
        // Abhi ka Indian time, padhne layak format mein
        String now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // Action = Telegram message (wahi Stage 1 wala service)
        telegramService.sendMessage(
                "Reminder from Automation Hub ⏰\n"
                        + "Time: " + now + "\n"
                        + "Aaj ka goal: ek project commit karna 🚀");
    }
}