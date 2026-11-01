package com.vansh.automation_hub.exception;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

// Kisi bhi channel se bhejne mein fail hone par. "retryable" batata hai ki dobara try karna faayda dega ya nahi
public class ChannelSendException extends RuntimeException {

    private final boolean retryable;

    public ChannelSendException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    // HTTP wale channels (Telegram, Discord) ka error-mapping ek jagah, taaki code repeat na ho
    public static ChannelSendException fromRestError(String channel, RestClientException e) {
        if (e instanceof HttpStatusCodeException http) {
            int code = http.getStatusCode().value();
            // 429 (rate limit) aur 5xx (server problem) retry se theek ho sakte hain.
            // Baaki 4xx (galat token/URL) retry se kabhi theek nahi honge.
            boolean retryable = code == 429 || code >= 500;
            return new ChannelSendException(channel + " returned HTTP " + code, retryable);
        }
        if (e instanceof ResourceAccessException) {
            // Net nahi, DNS fail ya timeout. Dhyan: e.getMessage() mein URL (secret) hota hai, isliye use nahi daalte
            return new ChannelSendException("Network problem or timeout while contacting " + channel, true);
        }
        return new ChannelSendException("Unexpected " + channel + " error: "
                + e.getClass().getSimpleName(), false);
    }
}