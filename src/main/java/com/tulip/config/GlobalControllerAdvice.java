package com.tulip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
    
    @Value("${api.base-url}")
    private String apiBaseUrl;
    
    @ModelAttribute("apiBaseUrl")
    public String apiBaseUrl() {
        return apiBaseUrl;
    }
}

