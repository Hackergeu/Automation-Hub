package com.vansh.automation_hub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TelegramService {

    // application.properties se token aata hai (jo environment variable se bhara gaya)
    @Value("${telegram.bot-token}")
    private String botToken;

    // Ye hamara "ghar ka address" hai, yaani message kis chat mein jayega
    @Value("${telegram.chat-id}")
    private String chatId;

    // RestTemplate = Spring ka tool jo doosri websites/APIs ko HTTP request bhejta hai
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String text) {
        // Telegram ka sendMessage endpoint, token URL ka hissa hota hai
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        // Request body: kis chat mein, aur kya text bhejna hai
        Map<String, String> body = Map.of(
                "chat_id", chatId,
                "text", text
        );

        try {
            // POST request bhejo; Telegram JSON mein jawab deta hai
            String response = restTemplate.postForObject(url, body, String.class);
            System.out.println("Telegram ka jawab: " + response);
        } catch (RestClientException e) {
            // Dhyan: poora error print nahi kar rahe, kyunki usme URL (aur token) aa sakta hai
            System.out.println("Message nahi gaya. Error type: " + e.getClass().getSimpleName());
        }
    }
}
