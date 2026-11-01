package com.vansh.automation_hub.channel;

import com.vansh.automation_hub.exception.ChannelSendException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class DiscordChannel implements MessageChannel {

    // Env variable se aata hai. Khali (default) ho toh channel unavailable
    @Value("${discord.webhook-url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    public DiscordChannel(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "DISCORD";
    }

    @Override
    public boolean isAvailable() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    @Override
    public void send(String target, String message) {
        if (!isAvailable()) {
            // Configuration ki galti retry se theek nahi hogi, isliye retryable = false
            throw new ChannelSendException("Discord webhook is not configured", false);
        }

        // Discord webhook ko JSON chahiye jisme "content" field mein message ho
        Map<String, String> body = Map.of("content", message);

        try {
            // Discord success par 204 No Content deta hai, isliye response ka koi kaam nahi
            restTemplate.postForObject(webhookUrl, body, String.class);
        } catch (RestClientException e) {
            throw ChannelSendException.fromRestError("Discord", e);
        }
    }
}