package com.tulip.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulip.dto.RatingDTO;
import com.tulip.entity.product.Rating;
import com.tulip.repository.RatingRepository;
import com.tulip.service.GoogleAIService;
import com.tulip.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ratings")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminRatingApiController {

    private final RatingService ratingService;
    private final RatingRepository ratingRepository;
    private final GoogleAIService googleAIService;
    private final ObjectMapper objectMapper;

    /**
     * Lấy danh sách tất cả đánh giá (có phân trang và filter)
     */
    @GetMapping
    public ResponseEntity<?> getAllRatings(
            @RequestParam(required = false) Integer stars,
            @RequestParam(required = false) Boolean hasReply,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<RatingDTO> ratingsPage = ratingService.getAllRatingsForAdmin(stars, hasReply, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ratingsPage.getContent());
            response.put("currentPage", ratingsPage.getNumber());
            response.put("totalPages", ratingsPage.getTotalPages());
            response.put("totalElements", ratingsPage.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách rating cho admin", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra"));
        }
    }

    /**
     * Admin phản hồi đánh giá
     */
    @PostMapping("/{ratingId}/reply")
    public ResponseEntity<?> replyToRating(
            @PathVariable Long ratingId,
            @RequestBody Map<String, String> request) {
        try {
            String replyContent = request.get("reply");
            if (replyContent == null || replyContent.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Nội dung phản hồi không được để trống"));
            }
            
            RatingDTO rating = ratingService.replyToRating(ratingId, replyContent);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Phản hồi đã được gửi thành công",
                "data", rating
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi phản hồi rating", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra"));
        }
    }

    /**
     * Admin toggle hiển thị đánh giá
     */
    @PostMapping("/{ratingId}/toggle-visibility")
    public ResponseEntity<?> toggleVisibility(@PathVariable Long ratingId) {
        try {
            RatingDTO rating = ratingService.toggleVisibility(ratingId);
            
            String message = rating.getIsVisible() ? 
                    "Đánh giá đã được hiển thị" : "Đánh giá đã được ẩn";
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "data", rating
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi toggle visibility rating", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra"));
        }
    }
    
    /**
     * AI-powered smart reply suggestions
     */
    @PostMapping("/{ratingId}/suggest-reply")
    public ResponseEntity<?> suggestReply(@PathVariable Long ratingId) {
        try {
            // Find rating
            Rating rating = ratingRepository.findById(ratingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
            
            // Generate suggestions using AI
            String aiResponse = googleAIService.generateReplySuggestions(
                rating.getStars(), 
                rating.getContent()
            );
            
            // Parse JSON response
            try {
                List<Map<String, String>> suggestions = objectMapper.readValue(
                    aiResponse, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
                
                // If we got less than 3 suggestions, fill with fallback
                if (suggestions.size() < 3) {
                    log.info("AI returned {} suggestions, filling with fallback", suggestions.size());
                    List<Map<String, String>> fallback = getFallbackSuggestions(rating.getStars());
                    while (suggestions.size() < 3 && suggestions.size() < fallback.size()) {
                        suggestions.add(fallback.get(suggestions.size()));
                    }
                }
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "suggestions", suggestions
                ));
                
            } catch (Exception parseError) {
                log.error("Failed to parse AI response: {}", aiResponse, parseError);
                
                // Return fallback suggestions
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "suggestions", getFallbackSuggestions(rating.getStars())
                ));
            }
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating reply suggestions", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "success", false, 
                        "message", "AI đang bận, vui lòng thử lại sau"
                    ));
        }
    }
    
    /**
     * Fallback suggestions when AI fails
     */
    private List<Map<String, String>> getFallbackSuggestions(int stars) {
        if (stars >= 4) {
            return List.of(
                Map.of("type", "Chuyên nghiệp", "text", "Cảm ơn bạn đã tin tưởng và lựa chọn TulipShop! 💝 Chúng mình rất vui khi bạn hài lòng với sản phẩm. Hẹn gặp lại bạn trong những lần mua sắm tiếp theo nhé!"),
                Map.of("type", "Thân thiện", "text", "Yay! Cảm ơn bạn nhiều nha 🥰 Được bạn khen là động lực để team mình cố gắng hơn nữa đấy! Chúc bạn luôn xinh đẹp và tự tin!"),
                Map.of("type", "Nhiệt tình", "text", "Wao! Cảm ơn bạn đã dành thời gian đánh giá! ⭐ Nếu có bất kỳ nhu cầu gì, đừng ngại inbox shop nhé. TulipShop luôn đồng hành cùng bạn! 💕")
            );
        } else {
            return List.of(
                Map.of("type", "Chuyên nghiệp", "text", "TulipShop xin lỗi vì trải nghiệm chưa được như mong đợi. 🙏 Bạn vui lòng inbox để shop hỗ trợ giải quyết vấn đề tốt nhất cho bạn nhé!"),
                Map.of("type", "Thân thiện", "text", "Shop rất tiếc khi bạn chưa hài lòng 😔 Bạn có thể cho shop biết thêm chi tiết để mình khắc phục được không ạ? Shop cam kết sẽ cải thiện!"),
                Map.of("type", "Nhiệt tình", "text", "Ôi không! Shop thật sự xin lỗi bạn 💔 Hãy để shop có cơ hội làm tốt hơn nhé! Inbox ngay để được hỗ trợ đổi trả hoặc giải quyết vấn đề nha!")
            );
        }
    }
    
    /**
     * Test endpoint to debug AI service
     */
    @GetMapping("/test-ai")
    public ResponseEntity<?> testAI() {
        try {
            log.info("🧪 Testing AI service...");
            
            String result = googleAIService.generateReplySuggestions(5, "Sản phẩm rất đẹp, chất lượng tốt!");
            
            log.info("✅ AI test successful");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AI service is working",
                "rawResponse", result
            ));
            
        } catch (Exception e) {
            log.error("❌ AI test failed", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "success", false,
                        "message", "AI service failed: " + e.getMessage(),
                        "error", e.getClass().getSimpleName()
                    ));
        }
    }
}
