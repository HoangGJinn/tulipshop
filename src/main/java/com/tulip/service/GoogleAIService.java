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
                "temperature", 0.7,
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 1024,
                "responseMimeType", "application/json"
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept-Charset", "UTF-8");
        
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
    
    /**
     * Generate smart reply suggestions for rating responses
     * @param stars Rating stars (1-5)
     * @param content Rating content from customer
     * @return JSON string with 3 reply suggestions
     */
    public String generateReplySuggestions(int stars, String content) {
        try {
            String prompt = buildReplySuggestionsPrompt(stars, content);
            
            String response = callGoogleAI(prompt);
            
            String extractedText = extractResponseContent(response);
            //log.info("FULL Extracted text from AI: {}", extractedText);
            
            // Clean and extract JSON
            extractedText = cleanJsonResponse(extractedText);
            //log.info("FULL Cleaned JSON: {}", extractedText);
            
            // Validate JSON before returning
            try {
                objectMapper.readTree(extractedText);
                //log.info("AI generated valid JSON reply suggestions");
                return extractedText;
            } catch (Exception e) {
                log.warn("AI response is not valid JSON, using fallback");
                log.warn("JSON parse error: {}", e.getMessage());
                log.warn("JSON length: {} chars", extractedText != null ? extractedText.length() : 0);
                log.warn("Full invalid JSON: {}", extractedText);
                return generateFallbackSuggestions(stars);
            }
            
        } catch (Exception e) {
            log.error("Error generating reply suggestions: {}", e.getMessage());
            log.warn("Using fallback suggestions instead");
            return generateFallbackSuggestions(stars);
        }
    }
    
    private String buildReplySuggestionsPrompt(int stars, String content) {
        String contentText = (content != null && !content.trim().isEmpty()) 
            ? content 
            : "Không có nội dung";
            
        String tone = (stars >= 4) 
            ? "grateful and welcoming"
            : "apologetic and helpful";
            
        return String.format("""
            Create 3 Vietnamese customer service replies for %d-star rating: "%s"
            
            Tone: %s
            Length: Max 25 words each
            Emojis: 1-2 per reply
            
            Return JSON array:
            [
              {"type":"Professional","text":"reply 1"},
              {"type":"Warm","text":"reply 2"},
              {"type":"Creative","text":"reply 3"}
            ]
            """, stars, contentText, tone);
    }
    
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Empty response from AI");
            return "[]";
        }
        
        response = response.trim();
        
        // Remove markdown code blocks
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        response = response.trim();
        
        // Find JSON array in the response
        int startIdx = response.indexOf('[');
        int endIdx = response.lastIndexOf(']');
        
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            response = response.substring(startIdx, endIdx + 1);
        } else if (startIdx != -1) {
            // JSON array started but not closed - try to fix it
            log.warn("JSON array not properly closed, attempting to fix");
            response = response.substring(startIdx);
            
            // Try to close incomplete JSON
            // Count open braces
            int openBraces = 0;
            int closeBraces = 0;
            int lastValidPos = response.length();
            
            for (int i = 0; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') openBraces++;
                if (c == '}') closeBraces++;
                
                // If we have balanced braces, mark this position
                if (openBraces > 0 && openBraces == closeBraces) {
                    lastValidPos = i + 1;
                }
            }
            
            // Truncate to last valid position and close array
            if (lastValidPos < response.length()) {
                response = response.substring(0, lastValidPos) + "]";
                log.info("Fixed truncated JSON, new length: {}", response.length());
            } else if (!response.endsWith("]")) {
                response = response + "]";
            }
        }
        
        // Remove trailing comma before closing bracket (invalid JSON)
        response = response.replaceAll(",\\s*]", "]");
        
        // Remove any text before [ or after ]
        response = response.trim();
        
        log.debug("Cleaned response length: {}", response.length());
        return response;
    }
    
    private String generateFallbackSuggestions(int stars) {
        if (stars >= 4) {
            return """
                [
                    {"type": "Chuyên nghiệp", "text": "Cảm ơn bạn đã tin tưởng và lựa chọn TulipShop! 💝 Chúng mình rất vui khi bạn hài lòng với sản phẩm. Hẹn gặp lại bạn trong những lần mua sắm tiếp theo nhé!"},
                    {"type": "Thân thiện", "text": "Yay! Cảm ơn bạn nhiều nha 🥰 Được bạn khen là động lực để team mình cố gắng hơn nữa đấy! Chúc bạn luôn xinh đẹp và tự tin!"},
                    {"type": "Nhiệt tình", "text": "Wao! Cảm ơn bạn đã dành thời gian đánh giá! ⭐ Nếu có bất kỳ nhu cầu gì, đừng ngại inbox shop nhé. TulipShop luôn đồng hành cùng bạn! 💕"}
                ]
                """;
        } else {
            return """
                [
                    {"type": "Chuyên nghiệp", "text": "TulipShop xin lỗi vì trải nghiệm chưa được như mong đợi. 🙏 Bạn vui lòng inbox để shop hỗ trợ giải quyết vấn đề tốt nhất cho bạn nhé!"},
                    {"type": "Thân thiện", "text": "Shop rất tiếc khi bạn chưa hài lòng 😔 Bạn có thể cho shop biết thêm chi tiết để mình khắc phục được không ạ? Shop cam kết sẽ cải thiện!"},
                    {"type": "Nhiệt tình", "text": "Ôi không! Shop thật sự xin lỗi bạn 💔 Hãy để shop có cơ hội làm tốt hơn nhé! Inbox ngay để được hỗ trợ đổi trả hoặc giải quyết vấn đề nha!"}
                ]
                """;
        }
    }
}
