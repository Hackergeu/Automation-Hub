package com.vansh.automation_hub.exception;

// Jab cron expression Spring ke format ke hisaab se galat ho
public class InvalidCronException extends RuntimeException {

    public InvalidCronException(String cron) {
        super("Invalid cron expression: '" + cron
                + "'. Use 6 fields: second minute hour day month weekday");
    }
}