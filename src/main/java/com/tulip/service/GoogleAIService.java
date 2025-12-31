package com.tulip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulip.config.GoogleAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAIService {
    
    private final GoogleAIConfig googleAIConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public CompletableFuture<String> generateResponse(String userMessage, String context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(userMessage, context);
                String response = callGoogleAI(prompt);
                return extractResponseContent(response);
            } catch (Exception e) {
                log.error("Error calling Google AI API", e);
                return generateFallbackResponse(userMessage);
            }
        });
    }
    
    private String buildPrompt(String userMessage, String context) {
        return String.format("""
            Bạn là nhân viên tư vấn thân thiện của một shop thời trang nữ.
            
            Thông tin shop (lấy từ hệ thống/DB) và ngữ cảnh cuộc trò chuyện:
            %s
            
            Tin nhắn của khách hàng: %s
            
            Yêu cầu khi trả lời:
            - Ưu tiên trả lời dựa trên thông tin shop/DB được cung cấp ở trên (chính sách, size, tồn kho, sản phẩm).
            - Nếu thiếu dữ liệu để kết luận (ví dụ thiếu số đo/chiều cao/cân nặng, hoặc sản phẩm không xác định), hãy hỏi lại 1-2 câu để làm rõ.
            - Trả lời tự nhiên như nhân viên shop, ngắn gọn, rõ ràng, có gợi ý size.
            - Không bịa đặt chính sách/size/tồn kho nếu trong phần thông tin shop không có.
            
            Trả lời bằng tiếng Việt, ngắn gọn và dễ hiểu.
            """, context, userMessage);
    }
    
    private String callGoogleAI(String prompt) {
        String url = googleAIConfig.getApiUrl() + "?key=" + googleAIConfig.getApiKey();
        
        Map<String, Object> requestBody = Map.of(
            "contents", java.util.List.of(
                Map.of(
                    "parts", java.util.List.of(
                        Map.of("text", prompt)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 1024
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                log.error("Google AI API returned status: {}", response.getStatusCode());
                throw new RuntimeException("API call failed");
            }
        } catch (Exception e) {
            log.error("Error calling Google AI API", e);
            throw e;
        }
    }
    
    private String extractResponseContent(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);
                    return firstPart.path("text").asText();
                }
            }
            
            log.warn("Could not extract response from Google AI: {}", jsonResponse);
            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này. Vui lòng thử lại sau.";
            
        } catch (Exception e) {
            log.error("Error parsing Google AI response", e);
            return "Xin lỗi, có lỗi xảy ra khi xử lý phản hồi. Vui lòng thử lại.";
        }
    }
    
    private String generateFallbackResponse(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("xin chào") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Xin chào! Tôi là trợ lý của Tulip Shop. Tôi có thể giúp gì cho bạn hôm nay? 🌷";
        }
        
        if (lowerMessage.contains("cảm ơn") || lowerMessage.contains("thank")) {
            return "Rất vui được giúp đỡ bạn! Nếu có câu hỏi nào khác, đừng ngần ngại hỏi nhé.";
        }
        
        if (lowerMessage.contains("tulip") || lowerMessage.contains("hoa tulip")) {
            return "Hoa tulip là một trong những loại hoa đẹp nhất của chúng tôi! Chúng tôi có nhiều màu sắc khác nhau như đỏ, hồng, vàng và trắng. Bạn muốn tìm loại hoa tulip nào ạ?";
        }
        
        if (lowerMessage.contains("giá") || lowerMessage.contains("bao nhiêu")) {
            return "Giá hoa của chúng tôi rất đa dạng, tùy thuộc vào loại hoa và kích thước. Bạn đang quan tâm đến loại hoa nào để tôi có thể báo giá chính xác nhất?";
        }
        
        return "Cảm ơn câu hỏi của bạn. Tôi đang tìm hiểu thông tin và sẽ trả lời sớm nhất có thể. Bạn có thể hỏi thêm về các loại hoa hoặc dịch vụ của Tulip Shop nhé!";
    }
}
