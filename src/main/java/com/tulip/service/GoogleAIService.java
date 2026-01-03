package com.tulip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulip.config.GoogleAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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
                // Giảm temperature để AI tập trung vào facts, bớt lan man
                return extractResponseContent(callGoogleAI(prompt, 0.4)); 
            } catch (Exception e) {
                log.error("Error calling Google AI API", e);
                // Khi API lỗi, dùng fallback thông minh với context
                return generateFallbackResponseWithContext(userMessage, context);
            }
        });
    }

    // --- PROMPT ĐƯỢC CẬP NHẬT ĐỂ SỬ DỤNG DỮ LIỆU DATABASE ---
    private String buildPrompt(String userMessage, String context) {
        return String.format("""
            VAI TRÒ:
            Bạn là "Trợ lý ảo Tulip" - nhân viên tư vấn chuyên nghiệp của Tulip Shop (thời trang nữ).
            
            🎯 NHIỆM VỤ CHÍNH:
            - Tư vấn sản phẩm ĐÚNG với dữ liệu có sẵn bên dưới
            - Trả lời chính sách, size, giá CỦA SHOP (không tự bịa)
            - Gợi ý outfit phù hợp với nhu cầu khách
            
            ⚠️ QUY TẮC VÀNG (BẮT BUỘC TUÂN THỦ):
            1. CHỈ TRẢ LỜI DỰA VÀO "DỮ LIỆU SHOP" BẾN DƯỚI
            2. Nếu sản phẩm KHÔNG CÓ trong danh sách → Nói "shop chưa có mẫu này" (không bịa)
            3. Nếu hỏi giá mà không có trong data → Nói "em check lại giúp chị nhé"
            4. Nếu hỏi vấn đề NGOÀI thời trang (toán, lập trình, tin tức...) → TỪ CHỐI LỊCH SỰ:
               "Dạ em chỉ là trợ lý thời trang của Tulip Shop, chưa hỗ trợ được vấn đề này ạ 😊"
            
            📊 DỮ LIỆU SHOP (Từ Database - Dữ liệu thực):
            %s
            
            💬 TIN NHẮN KHÁCH: "%s"
            
            📝 CÁCH TRẢ LỜI:
            - Nếu khách hỏi "có áo gì không?" → Liệt kê 3-5 MẪU CỤ THỂ từ danh sách trên
            - Nếu hỏi giá → Trả lời CHÍNH XÁC giá trong data (có discount thì nói luôn)
            - Nếu hỏi size → Dùng bảng size ở trên + HỎI CHIỀU CAO/CÂN NẶNG nếu khách chưa cho
            - Nếu khách cho số đo → TƯ VẤN SIZE CỤ THỂ (S/M/L/XL)
            - Giọng văn: Ngọt ngào, thân thiện, dùng "dạ", "ạ", "chị", "nàng"
            - Độ dài: 2-4 câu là đủ, đừng quá dài
            
            ✅ VÍ DỤ TRẢ LỜI TỐT:
            Khách: "Có áo công sở không?"
            Bot: "Dạ có ạ! Shop đang có mấy mẫu này chị nhé:
            1. Áo Sơ Mi Trắng Công Sở - 350k (giảm còn 280k)
            2. Áo Kiểu Xanh Navy Thanh Lịch - 420k
            Chị thích mẫu nào để em tư vấn size ạ? 🥰"
            
            BẮT ĐẦU TRẢ LỜI (chỉ trả lời nội dung, không thêm meta-text):
            """, context, userMessage);
    }

    // Cho phép truyền temperature vào để linh hoạt
    private String callGoogleAI(String prompt, double temperature) {
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
                "temperature", temperature, // Chỉ số sáng tạo (thấp = chính xác, cao = sáng tạo)
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 800 // Giới hạn độ dài trả lời cho ngắn gọn
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
    
    // Giữ nguyên logic cũ cho fallback
    private String callGoogleAI(String prompt) {
        return callGoogleAI(prompt, 0.7); // Mặc định cho các task sáng tạo
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
            return "Dạ hiện tại hệ thống đang quá tải, chị chờ em một xíu nhé!";

        } catch (Exception e) {
            log.error("Error parsing Google AI response", e);
            return "Dạ có chút lỗi kết nối, chị hỏi lại giúp em nha!";
        }
    }

    /**
     * Fallback response thông minh - Sử dụng context từ database
     * Khi Gemini API lỗi (quota, network...), vẫn trả lời được dựa trên dữ liệu thực
     */
    private String generateFallbackResponseWithContext(String userMessage, String context) {
        String lowerMessage = userMessage.toLowerCase();
        
        log.info("🛡️ Sử dụng fallback response với context ({} ký tự)", context != null ? context.length() : 0);
        
        // 1. Trích xuất danh sách sản phẩm từ context
        List<String> products = extractProductsFromContext(context);
        
        // 2. Hỏi về sản phẩm
        if (lowerMessage.matches(".*(có gì|bán gì|sản phẩm|mẫu|áo|váy|quần|đầm).*") && !products.isEmpty()) {
            StringBuilder response = new StringBuilder("Dạ shop đang có mấy mẫu này ạ:\n\n");
            int count = Math.min(5, products.size());
            for (int i = 0; i < count; i++) {
                response.append(products.get(i)).append("\n");
            }
            response.append("\nChị quan tâm mẫu nào để em tư vấn thêm nhé! 🥰");
            return response.toString();
        }
        
        // 3. Hỏi giá
        if (lowerMessage.matches(".*(giá|bao nhiêu|nhiêu tiền).*") && !products.isEmpty()) {
            return "Dạ em thấy có mấy sản phẩm này ạ:\n\n" + 
                   String.join("\n", products.subList(0, Math.min(3, products.size()))) +
                   "\n\nChị thích mẫu nào ạ? 💕";
        }
        
        // 4. Hỏi size
        if (lowerMessage.matches(".*(size|cỡ|kích thước|chiều cao|cân nặng).*")) {
            return """
                Dạ về size, shop có bảng size chuẩn ạ:
                
                📏 S: 45-50kg, cao 1m50-1m58
                📏 M: 51-55kg, cao 1m58-1m65
                📏 L: 56-62kg, cao 1m65-1m70
                📏 XL: 63-70kg, cao 1m70+
                
                Chị cho em biết chiều cao cân nặng để tư vấn chính xác hơn nhé! 😊
                """;
        }
        
        // 5. Hỏi chính sách
        if (lowerMessage.matches(".*(đổi trả|bảo hành|vận chuyển|ship|giao hàng|thanh toán).*")) {
            return """
                Dạ shop có các chính sách này ạ:
                
                🔄 Đổi size miễn phí trong 7 ngày
                🛡️ Bảo hành 30 ngày lỗi nhà sản xuất
                🚚 Ship toàn quốc (1-2 ngày nội thành, 3-5 ngày tỉnh)
                💳 COD, chuyển khoản, ví điện tử
                
                Chị cần biết thêm chi tiết gì không ạ? 💕
                """;
        }
        
        // 6. Chào hỏi
        if (lowerMessage.matches(".*(xin chào|hello|hi|chào).*")) {
            if (!products.isEmpty()) {
                return "Xin chào chị! 🥰 Em là trợ lý của Tulip Shop ạ.\n\n" +
                       "Shop đang có nhiều mẫu đẹp lắm, chị xem qua nhé:\n" +
                       String.join("\n", products.subList(0, Math.min(3, products.size()))) +
                       "\n\nChị thích mẫu nào ạ?";
            }
            return "Xin chào chị! 🥰 Em là trợ lý của Tulip Shop. Em có thể tư vấn về sản phẩm, size, chính sách cho chị ạ!";
        }
        
        // 7. Fallback mặc định (có context)
        if (!products.isEmpty()) {
            return "Dạ em đã nhận được câu hỏi của chị! Shop đang có nhiều mẫu đẹp lắm ạ:\n\n" +
                   String.join("\n", products.subList(0, Math.min(3, products.size()))) +
                   "\n\nChị cần tư vấn gì thêm không ạ? 💕";
        }
        
        // 8. Fallback cuối cùng (không có context)
        return "Dạ em đã nhận tin nhắn của chị rồi ạ! Chị cần tư vấn về sản phẩm, size hay chính sách nào không ạ? 😊";
    }
    
    /**
     * Trích xuất danh sách sản phẩm từ context
     */
    private List<String> extractProductsFromContext(String context) {
        List<String> products = new ArrayList<>();
        if (context == null || !context.contains("DANH SÁCH SẢN PHẨM")) {
            return products;
        }
        
        try {
            // Tìm section sản phẩm
            int startIdx = context.indexOf("DANH SÁCH SẢN PHẨM");
            if (startIdx == -1) return products;
            
            String productSection = context.substring(startIdx);
            String[] lines = productSection.split("\n");
            
            StringBuilder currentProduct = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                // Dòng bắt đầu bằng số = sản phẩm mới
                if (line.matches("^\\d+\\..*")) {
                    if (currentProduct.length() > 0) {
                        products.add(currentProduct.toString().trim());
                    }
                    currentProduct = new StringBuilder(line);
                } else if (line.startsWith("💰") && currentProduct.length() > 0) {
                    // Thêm giá vào sản phẩm hiện tại
                    currentProduct.append(" - ").append(line.replace("💰 Giá: ", ""));
                }
            }
            
            // Thêm sản phẩm cuối
            if (currentProduct.length() > 0) {
                products.add(currentProduct.toString().trim());
            }
            
        } catch (Exception e) {
            log.error("Error extracting products from context", e);
        }
        
        return products;
    }
    
    // Giữ lại old fallback cho các trường hợp không có context
    private String generateFallbackResponse(String userMessage) {
        return generateFallbackResponseWithContext(userMessage, "");
    }

    // --- PHẦN GỢI Ý ĐÁNH GIÁ (GIỮ NGUYÊN HOẶC TỐI ƯU NHẸ) ---
    public String generateReplySuggestions(int stars, String content) {
        try {
            String prompt = buildReplySuggestionsPrompt(stars, content);
            // Dùng temperature cao hơn (0.7) vì cần sáng tạo
            String response = callGoogleAI(prompt, 0.7); 
            
            String extractedText = extractResponseContent(response);
            extractedText = cleanJsonResponse(extractedText);
            
            try {
                objectMapper.readTree(extractedText);
                return extractedText;
            } catch (Exception e) {
                return generateFallbackSuggestions(stars);
            }
        } catch (Exception e) {
            return generateFallbackSuggestions(stars);
        }
    }

    // ... (Các hàm buildReplySuggestionsPrompt, cleanJsonResponse, generateFallbackSuggestions giữ nguyên như cũ của bạn) ...
    // Bạn nhớ copy lại các hàm đó vào đây nhé để file hoàn chỉnh.
    
    private String buildReplySuggestionsPrompt(int stars, String content) {
        // ... (Giữ nguyên code cũ của bạn)
        String contentText = (content != null && !content.trim().isEmpty()) ? content : "Không có nội dung";
        String tone = (stars >= 4) ? "grateful and welcoming" : "apologetic and helpful";
        return String.format("""
            Create 3 Vietnamese customer service replies for %d-star rating: "%s"
            Tone: %s
            Length: Max 25 words each
            Emojis: 1-2 per reply
            Return JSON array: [{"type":"Professional","text":"..."},{"type":"Warm","text":"..."},{"type":"Creative","text":"..."}]
            """, stars, contentText, tone);
    }

    private String cleanJsonResponse(String response) {
       // ... (Giữ nguyên code xử lý JSON cũ của bạn vì nó đã rất tốt)
       if (response == null) return "[]";
       response = response.trim();
       if (response.startsWith("```json")) response = response.substring(7);
       if (response.startsWith("```")) response = response.substring(3);
       if (response.endsWith("```")) response = response.substring(0, response.length() - 3);
       return response.trim();
    }

    private String generateFallbackSuggestions(int stars) {
        // ... (Giữ nguyên code cũ của bạn)
        if (stars >= 4) {
            return """
                [
                    {"type": "Chuyên nghiệp", "text": "Cảm ơn bạn đã tin tưởng TulipShop! 💝 Hẹn gặp lại bạn lần sau nhé!"},
                    {"type": "Thân thiện", "text": "Yay! Cảm ơn bạn nhiều nha 🥰 Chúc bạn luôn xinh đẹp!"},
                    {"type": "Nhiệt tình", "text": "Wao! Cảm ơn đánh giá của bạn! ⭐ Có cần hỗ trợ gì cứ inbox shop nha! 💕"}
                ]
                """;
        } else {
            return """
                [
                    {"type": "Chuyên nghiệp", "text": "TulipShop xin lỗi vì trải nghiệm chưa tốt. 🙏 Bạn inbox để shop hỗ trợ ngay nhé!"},
                    {"type": "Thân thiện", "text": "Shop rất tiếc 😔 Bạn cho shop biết thêm chi tiết để khắc phục nha!"},
                    {"type": "Nhiệt tình", "text": "Ôi không! Xin lỗi bạn 💔 Inbox shop ngay để được đền bù nhé!"}
                ]
                """;
        }
    }
}