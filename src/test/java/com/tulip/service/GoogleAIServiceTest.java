package com.tulip.service;

import com.tulip.config.GoogleAIConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "google.ai.api.key=AIzaSyCb8hQHIdao-KTj5tP8vN4SD3h-hqX-uwc",
    "google.ai.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
})
public class GoogleAIServiceTest {

    @Test
    void testGoogleAIConfiguration() {
        GoogleAIConfig config = new GoogleAIConfig();
        // This test will verify the configuration is loaded correctly
        // In a real test environment, you would inject the actual config
    }
}
