package com.vansh.automation_hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    // Ek hi RestTemplate sab channels share karenge. @Bean = Spring isse bana ke inject karega
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));  // connection banane ka max time
        factory.setReadTimeout(Duration.ofSeconds(10));    // jawab aane ka max time
        return new RestTemplate(factory);
    }
}