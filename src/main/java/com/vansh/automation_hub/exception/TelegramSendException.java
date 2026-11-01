package com.vansh.automation_hub.exception;

// Telegram bhejne mein fail hone par. "retryable" batata hai ki dobara try karna faayda dega ya nahi
public class TelegramSendException extends RuntimeException {

    private final boolean retryable;

    public TelegramSendException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}