package com.vansh.automation_hub.service;

import com.vansh.automation_hub.exception.TelegramSendException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    private final RestTemplate restTemplate;

    public TelegramService() {
        // Timeouts: bina inke, net atakne par request hamesha intezaar karti reh sakti hai
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));   // connection banane ka max time
        factory.setReadTimeout(Duration.ofSeconds(10));     // jawab aane ka max time
        this.restTemplate = new RestTemplate(factory);
    }

    // Success par kuch nahi lautata. Fail hone par TelegramSendException phenkta hai.
    public void sendMessage(String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, String> body = Map.of(
                "chat_id", chatId,
                "text", text
        );

        try {
            restTemplate.postForObject(url, body, String.class);

        } catch (HttpStatusCodeException e) {
            // Telegram ne jawab diya par error code ke saath (4xx ya 5xx)
            int code = e.getStatusCode().value();
            // 429 (rate limit) aur 5xx (server problem) thodi der baad theek ho sakte hain.
            // Baaki 4xx (galat token, galat chat id) retry se theek nahi honge.
            boolean retryable = code == 429 || code >= 500;
            throw new TelegramSendException("Telegram returned HTTP " + code, retryable);

        } catch (ResourceAccessException e) {
            // Internet nahi, DNS fail, ya timeout. Transient hai, retry karo.
            // Dhyan: e.getMessage() mein URL (aur token) hota hai, isliye use message mein nahi daalte
            throw new TelegramSendException("Network problem or timeout while contacting Telegram", true);

        } catch (RestClientException e) {
            // Koi aur ajeeb error: bina soche retry mat karo
            throw new TelegramSendException("Unexpected error: " + e.getClass().getSimpleName(), false);
        }
    }
}