package com.tulip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:google-ai.properties")
public class GoogleAIConfig {
    
    @Value("${google.ai.api.key}")
    private String apiKey;
    
    @Value("${google.ai.api.url}")
    private String apiUrl;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
}
