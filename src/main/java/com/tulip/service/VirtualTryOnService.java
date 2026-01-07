package com.tulip.service;

import com.tulip.dto.request.AiTryOnRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
public class VirtualTryOnService {

    private final String AI_SERVICE_URL = "https://unprocreated-mica-petrifiedly.ngrok-free.dev/try-on";

    public String tryOn(String personUrl, String clothUrl, String category) {
        RestTemplate restTemplate = new RestTemplate();
        AiTryOnRequest request = new AiTryOnRequest(personUrl, clothUrl, category);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(AI_SERVICE_URL, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && "success".equals(body.get("status"))) {
                return (String) body.get("result_url");
            }
            throw new RuntimeException("Lỗi AI: " + (body != null ? body.get("message") : "Không xác định"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không kết nối được AI Service: " + e.getMessage());
        }
    }
}