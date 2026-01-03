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
    
    /**
     * Generate product description using Gemini API with URL reference (NO Base64)
     * @param productName Product name
     * @param imageUrl Product image URL from Cloudinary (already optimized to 512px)
     * @param neckline Neckline type
     * @param material Material type
     * @param sleeveType Sleeve type
     * @param brand Brand name
     * @return HTML formatted product description
     */
    public String generateProductDescription(String productName, String imageUrl, 
                                             String neckline, String material, 
                                             String sleeveType, String brand) {
        int maxRetries = 2;
        int retryCount = 0;
        long waitTime = 2000; // Bắt đầu với 2 giây
        
        while (retryCount <= maxRetries) {
            try {
                log.info("🤖 Calling Gemini API (Attempt {}/{}) with URL: {}", 
                        retryCount + 1, maxRetries + 1, imageUrl);
                
                String prompt = buildProductDescriptionPrompt(productName, imageUrl, 
                                                             neckline, material, sleeveType, brand);
                
                // Gọi API với URL (KHÔNG dùng Base64)
                String response = callGoogleAIWithUrlContext(prompt, imageUrl);
                String extractedText = extractResponseContent(response);
                
                // Clean HTML response
                extractedText = cleanHtmlResponse(extractedText);
                
                log.info("✅ AI generated product description successfully on attempt {}", retryCount + 1);
                return extractedText;
                
            } catch (Exception e) {
                // Kiểm tra lỗi 429 (Too Many Requests)
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        log.warn("⚠️ Rate limit exceeded (429 Too Many Requests). Waiting {}ms before retry... (Attempt {}/{})", 
                                waitTime, retryCount, maxRetries);
                        try {
                            Thread.sleep(waitTime);
                            waitTime *= 2; // Exponential backoff: 2s -> 4s -> 8s
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("❌ Retry interrupted");
                            break;
                        }
                        continue; // Thử lại
                    } else {
                        log.error("❌ Max retries ({}) exceeded for 429 error", maxRetries);
                    }
                }
                
                // Nếu hết retry hoặc lỗi khác, dùng fallback
                log.error("❌ Error generating product description after {} attempts: {}", 
                         retryCount + 1, e.getMessage());
                log.info("🔄 Using template-based fallback description");
                return generateFallbackProductDescription(productName, neckline, material, sleeveType, brand);
            }
        }
        
        // Fallback cuối cùng (không nên đến đây)
        log.warn("⚠️ Reached end of retry loop, using fallback");
        return generateFallbackProductDescription(productName, neckline, material, sleeveType, brand);
    }
    
    private String buildProductDescriptionPrompt(String productName, String imageUrl,
                                                 String neckline, String material,
                                                 String sleeveType, String brand) {
        return String.format("""
            Bạn là Giám đốc Sáng tạo của thương hiệu thời trang cao cấp TulipShop.
            
            NHIỆM VỤ: Viết mô tả sản phẩm dựa trên các thuộc tính sau:
            - Tên sản phẩm: %s
            - Kiểu cổ: %s
            - Chất liệu: %s
            - Kiểu tay áo: %s
            - Thương hiệu: %s
            
            YÊU CẦU VỀ NỘI DUNG:
            
            1. PHẦN MỞ ĐẦU (The Hook):
               - Mô tả 'thần thái' của sản phẩm dựa trên tên và thuộc tính
               - Sử dụng ngôn từ hoa mỹ, khơi gợi cảm xúc
               - Ví dụ: "Sự mềm mại của lụa, nét thanh tao của đường cắt..."
            
            2. ĐIỂM NHẤN THIẾT KẾ (Highlights):
               - Viết 3-4 dòng về sự tinh tế của kiểu cổ %s và tay áo %s
               - Nhấn mạnh cách chúng tôn dáng người mặc
               - Tập trung vào ưu điểm của thiết kế
            
            3. TRẢI NGHIỆM CHẤT LIỆU:
               - Mô tả cảm giác khi chạm vào vải %s
               - Đề cập: thấm hút, mịn màng, bền bỉ, thoải mái
               - Lợi ích khi sử dụng chất liệu này
            
            4. GỢI Ý PHỐI ĐỒ (Styling Tips):
               - Đóng vai Stylist tư vấn cách phối món đồ này
               - Gợi ý phụ kiện/giày dép cho: đi làm, đi tiệc, dạo phố
               - Tạo cảm hứng cho khách hàng
            
            YÊU CẦU VỀ ĐỊNH DẠNG (BẮT BUỘC):
            - Trả về mã HTML thuần, sử dụng Bootstrap 5
            - Cấu trúc:
              <div class='product-story'>
                <h3 class='text-uppercase fw-bold border-bottom pb-2 mb-3'>Câu chuyện sản phẩm</h3>
                <p class='lead'>Phần mở đầu hấp dẫn...</p>
                <img src='%s' class='img-fluid rounded shadow-sm my-4' alt='%s'>
                <h4 class='fw-bold mt-4 mb-3'>Điểm nhấn thiết kế</h4>
                <ul class='list-unstyled'>
                  <li class='mb-2'>✨ Điểm nổi bật 1</li>
                  <li class='mb-2'>✨ Điểm nổi bật 2</li>
                  <li class='mb-2'>✨ Điểm nổi bật 3</li>
                </ul>
                <h4 class='fw-bold mt-4 mb-3'>Trải nghiệm chất liệu</h4>
                <p>Mô tả chi tiết về chất liệu...</p>
                <h4 class='fw-bold mt-4 mb-3'>Gợi ý phối đồ</h4>
                <p>Styling tips cụ thể...</p>
              </div>
            
            - KHÔNG thêm ```html hoặc markdown
            - Chỉ trả về HTML thuần
            - Giọng văn: Sang trọng, chuyên nghiệp, tiếng Việt
            - Ngắn gọn, súc tích để tiết kiệm tokens
            """, 
            productName, neckline, material, sleeveType, brand,
            neckline, sleeveType, material, imageUrl, productName);
    }
    
    /**
     * Call Gemini API with image URL reference (NO Base64 inline_data)
     * Sử dụng URL trong text prompt để giảm payload size và token consumption
     * @param prompt Text prompt with product details
     * @param imageUrl Optimized Cloudinary URL (512px)
     * @return API response JSON string
     */
    private String callGoogleAIWithUrlContext(String prompt, String imageUrl) {
        String url = googleAIConfig.getApiUrl() + "?key=" + googleAIConfig.getApiKey();
        
        // Kết hợp prompt với URL ảnh (chỉ dẫn cho AI, KHÔNG tải ảnh)
        String fullPrompt = String.format("""
            %s
            
            📸 HÌNH ẢNH SẢN PHẨM: %s
            
            Lưu ý: Hãy tạo mô tả dựa trên các thuộc tính đã cung cấp (tên, cổ áo, chất liệu, tay áo, thương hiệu).
            Không cần phân tích chi tiết ảnh, chỉ cần tham khảo để tạo nội dung phù hợp.
            """, prompt, imageUrl);
        
        // Build request body - CHỈ dùng text, KHÔNG dùng inline_data hay tools
        Map<String, Object> requestBody = Map.of(
            "contents", java.util.List.of(
                Map.of(
                    "parts", java.util.List.of(
                        Map.of("text", fullPrompt)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", 0.8,
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 2048
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            log.debug("📤 Sending request to Gemini API (text-only, no Base64)");
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("✅ Received 200 OK from Gemini API");
                return response.getBody();
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("⚠️ Received 429 Too Many Requests from Gemini API");
                throw new RuntimeException("429 - Rate limit exceeded");
            } else {
                log.error("❌ Google AI API returned unexpected status: {}", response.getStatusCode());
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("⚠️ HttpClientErrorException: 429 Too Many Requests");
                throw new RuntimeException("429 - Rate limit exceeded");
            }
            log.error("❌ HttpClientErrorException calling Gemini API: {} - {}", 
                     e.getStatusCode(), e.getMessage());
            throw new RuntimeException("API call failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error calling Gemini API", e);
            throw new RuntimeException("API call failed: " + e.getMessage());
        }
    }
    
    private String cleanHtmlResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }
        
        response = response.trim();
        
        // Remove markdown code blocks
        if (response.startsWith("```html")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        return response.trim();
    }
    
    private String generateFallbackProductDescription(String productName, String neckline, 
                                                     String material, String sleeveType, String brand) {
        return String.format("""
            <div class='product-story'>
                <h3 class='text-uppercase fw-bold border-bottom pb-2 mb-3'>Giới thiệu sản phẩm</h3>
                <p class='lead'>%s - Sự lựa chọn hoàn hảo cho phong cách hiện đại và thanh lịch.</p>
                
                <h4 class='fw-bold mt-4 mb-3'>Đặc điểm nổi bật</h4>
                <ul class='list-unstyled'>
                    <li class='mb-2'>✨ Thiết kế %s tôn dáng, phù hợp với nhiều dáng người</li>
                    <li class='mb-2'>✨ Chất liệu %s cao cấp, mang lại cảm giác thoải mái</li>
                    <li class='mb-2'>✨ %s tinh tế, dễ dàng phối đồ</li>
                    <li class='mb-2'>✨ Thương hiệu %s - Cam kết chất lượng</li>
                </ul>
                
                <h4 class='fw-bold mt-4 mb-3'>Hướng dẫn sử dụng</h4>
                <p>Sản phẩm phù hợp cho nhiều dịp khác nhau: đi làm, dạo phố, gặp gỡ bạn bè. 
                Dễ dàng phối cùng quần jeans, chân váy hoặc quần tây để tạo nên phong cách riêng.</p>
                
                <h4 class='fw-bold mt-4 mb-3'>Chăm sóc sản phẩm</h4>
                <p>Giặt máy ở nhiệt độ thường, không sử dụng chất tẩy mạnh. 
                Phơi nơi thoáng mát, tránh ánh nắng trực tiếp để bảo quản màu sắc lâu dài.</p>
            </div>
            """, productName, neckline, material, sleeveType, brand);
    }
}
