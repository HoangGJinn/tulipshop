package com.tulip.controller.api;

import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingRequest;
import com.tulip.dto.RatingStatistics;
import com.tulip.entity.User;
import com.tulip.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
public class RatingApiController {

    private final RatingService ratingService;

    /**
     * Submit đánh giá sản phẩm
     */
    @PostMapping
    public ResponseEntity<?> submitRating(
            @Valid @ModelAttribute RatingRequest request,
            @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Vui lòng đăng nhập để đánh giá"));
            }

            RatingDTO rating = ratingService.submitRating(request, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đánh giá của bạn đã được gửi thành công!");
            response.put("data", rating);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Lỗi submit rating: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi submit rating", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra, vui lòng thử lại sau"));
        }
    }

    /**
     * Lấy danh sách đánh giá của sản phẩm
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductRatings(@PathVariable Long productId) {
        try {
            List<RatingDTO> ratings = ratingService.getProductRatings(productId);
            return ResponseEntity.ok(Map.of("success", true, "data", ratings));
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách rating", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra"));
        }
    }

    /**
     * Lấy thống kê đánh giá của sản phẩm
     */
    @GetMapping("/product/{productId}/statistics")
    public ResponseEntity<?> getProductRatingStatistics(@PathVariable Long productId) {
        try {
            RatingStatistics stats = ratingService.getProductRatingStatistics(productId);
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            log.error("Lỗi lấy thống kê rating", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra"));
        }
    }

    /**
     * Kiểm tra user có thể đánh giá sản phẩm không
     */
    @GetMapping("/can-rate")
    public ResponseEntity<?> canUserRateProduct(
            @RequestParam Long productId,
            @RequestParam Long orderId,
            @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.ok(Map.of("success", true, "canRate", false));
            }

            boolean canRate = ratingService.canUserRateProduct(user.getId(), productId, orderId);
            return ResponseEntity.ok(Map.of("success", true, "canRate", canRate));
        } catch (Exception e) {
            log.error("Lỗi kiểm tra quyền đánh giá", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra"));
        }
    }
}
