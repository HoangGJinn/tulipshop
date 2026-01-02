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
            
            // Clean markdown code blocks if present
            extractedText = cleanJsonResponse(extractedText);
            
            log.info("AI generated reply suggestions for {} stars rating", stars);
            return extractedText;
            
        } catch (Exception e) {
            log.error("Error generating reply suggestions", e);
            return generateFallbackSuggestions(stars);
        }
    }
    
    private String buildReplySuggestionsPrompt(int stars, String content) {
        String contentText = (content != null && !content.trim().isEmpty()) 
            ? content 
            : "Không có nội dung cụ thể";
            
        return String.format("""
            Bạn là nhân viên CSKH chuyên nghiệp của 'TulipShop' (shop thời trang nữ cao cấp).
            
            Khách hàng vừa đánh giá %d sao với nội dung: "%s"
            
            Hãy viết 3 mẫu câu trả lời ngắn gọn (dưới 50 từ mỗi câu), lịch sự, giọng văn thân thiện, có emoji phù hợp.
            
            YÊU CẦU:
            - Nếu đánh giá 4-5 sao: Cảm ơn, khuyến khích, mời quay lại
            - Nếu đánh giá 1-3 sao: Xin lỗi, thể hiện quan tâm, đề xuất giải pháp
            - Mỗi câu trả lời phải có phong cách khác nhau (chuyên nghiệp, thân thiện, nhiệt tình)
            - Sử dụng emoji phù hợp nhưng không quá nhiều (1-2 emoji/câu)
            
            Trả về kết quả CHỈ LÀ JSON array thuần túy, KHÔNG có markdown, KHÔNG có ```json, theo định dạng:
            [
                {"type": "Chuyên nghiệp", "text": "..."},
                {"type": "Thân thiện", "text": "..."},
                {"type": "Nhiệt tình", "text": "..."}
            ]
            
            CHÚ Ý: Chỉ trả về JSON array, không thêm bất kỳ text nào khác.
            """, stars, contentText);
    }
    
    private String cleanJsonResponse(String response) {
        if (response == null) return "[]";
        
        // Remove markdown code blocks
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        return response.trim();
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
