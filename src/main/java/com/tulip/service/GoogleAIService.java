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
            Bạn là nhân viên tư vấn thân thiện của Tulip Shop (shop thời trang nữ).

            QUY TẮC BẮT BUỘC:
            - Chỉ trả lời dựa trên thông tin trong mục THÔNG TIN SHOP VÀ CHÍNH SÁCH.
            - Không được tự bịa chính sách, số liệu, giá, thời gian.
            - Nếu thiếu dữ liệu để trả lời chắc chắn, hãy hỏi lại 1-2 câu ngắn để làm rõ (ví dụ: chiều cao/cân nặng, mẫu sản phẩm, khu vực giao hàng).
            - Nếu khách hỏi chính sách/size, ưu tiên trích dẫn ngắn gọn từ thông tin được cung cấp.
            
            THÔNG TIN SHOP VÀ CHÍNH SÁCH (dùng để trả lời khách):
            %s
            
            Tin nhắn của khách hàng: %s
            
            Hướng dẫn trả lời:
            - Dựa vào THÔNG TIN SHOP VÀ CHÍNH SÁCH ở trên để trả lời chính xác.
            - Nếu khách hỏi size, hãy dùng bảng size trong thông tin shop và có thể hỏi thêm chiều cao/cân nặng để tư vấn size phù hợp.
            - Nếu khách hỏi chính sách, trích dẫn từ thông tin shop một cách ngắn gọn.
            - Nếu khách hỏi sản phẩm, ưu tiên gợi ý sản phẩm nếu có trong thông tin.
            - Giọng văn thân thiện, chuyên nghiệp, như nhân viên tư vấn thực tế.
            - Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu.
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
                "temperature", 0.4,
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
            return "Xin chào! Mình là trợ lý của Tulip Shop. Mình có thể hỗ trợ bạn về size, chính sách đổi trả/bảo hành, thanh toán, vận chuyển hoặc tư vấn sản phẩm.";
        }
        
        if (lowerMessage.contains("cảm ơn") || lowerMessage.contains("thank")) {
            return "Rất vui được giúp đỡ bạn! Nếu có câu hỏi nào khác, đừng ngần ngại hỏi nhé.";
        }
        
        if (lowerMessage.contains("giá") || lowerMessage.contains("bao nhiêu")) {
            return "Bạn đang xem sản phẩm/mẫu nào ạ? Bạn gửi tên sản phẩm hoặc link/mã sản phẩm, mình sẽ báo giá và chương trình khuyến mãi (nếu có).";
        }
        
        return "Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn?";
    }
}
