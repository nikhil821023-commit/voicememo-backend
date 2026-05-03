package com.voicememo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WhisperConfig {

    @Value("${app.whisper.api-key}")
    private String apiKey;

    @Value("${app.whisper.api-url}")
    private String apiUrl;

    @Bean
    public WebClient whisperWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}