package com.vansh.automation_hub.channel;

import com.vansh.automation_hub.exception.ChannelSendException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// @Component = Spring ise khud dhundh ke MessageChannel ki list mein daal dega
@Component
public class TelegramChannel implements MessageChannel {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    private final RestTemplate restTemplate;

    public TelegramChannel(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "TELEGRAM";
    }

    // Telegram mein target ki zarurat nahi, hamesha apna chat id use hota hai
    @Override
    public void send(String target, String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, String> body = Map.of("chat_id", chatId, "text", message);

        try {
            restTemplate.postForObject(url, body, String.class);
        } catch (RestClientException e) {
            throw ChannelSendException.fromRestError("Telegram", e);
        }
    }
}