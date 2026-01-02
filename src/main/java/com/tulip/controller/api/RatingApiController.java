package com.tulip.controller.api;

import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingRequest;
import com.tulip.dto.RatingStatistics;
import com.tulip.entity.Order;
import com.tulip.entity.User;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.repository.OrderRepository;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
public class RatingApiController {

    private final RatingService ratingService;
    private final OrderRepository orderRepository;
    private final com.tulip.repository.UserRepository userRepository;

    /**
     * Submit đánh giá sản phẩm
     */
    @PostMapping
    public ResponseEntity<?> submitRating(
            @Valid @ModelAttribute RatingRequest request,
            @AuthenticationPrincipal User user) {
        try {
            // Thử lấy user từ nhiều nguồn nếu @AuthenticationPrincipal không hoạt động
            if (user == null) {
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null && auth.isAuthenticated() && 
                    !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof User) {
                        user = (User) principal;
                    } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                        String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                        Optional<User> userOpt = userRepository.findByEmail(email);
                        if (userOpt.isPresent()) {
                            user = userOpt.get();
                            log.info("✅ Loaded user from DB: {}", email);
                        }
                    }
                }
            }
            
            if (user == null) {
                log.warn("❌ User not authenticated for rating submission");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Vui lòng đăng nhập để đánh giá"));
            }

            log.info("📝 User {} submitting rating for product {} in order {}", 
                     user.getEmail(), request.getProductId(), request.getOrderId());
            
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
    
    /**
     * Debug endpoint - Kiểm tra chi tiết quyền đánh giá
     */
    @GetMapping("/debug/can-rate")
    public ResponseEntity<?> debugCanRate(
            @RequestParam Long productId,
            @RequestParam Long orderId,
            @AuthenticationPrincipal User user,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("productId", productId);
        debug.put("orderId", orderId);
        
        // Thử lấy user từ nhiều nguồn
        if (user == null) {
            // Thử lấy từ SecurityContext
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated() && 
                !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                Object principal = auth.getPrincipal();
                if (principal instanceof User) {
                    user = (User) principal;
                    debug.put("userSource", "SecurityContext-User");
                } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    // Nếu là UserDetails, load User entity từ DB
                    String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    debug.put("userEmail", email);
                    debug.put("userSource", "UserDetails");
                    
                    // Load User từ DB
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        user = userOpt.get();
                        debug.put("userLoadedFromDB", true);
                    } else {
                        debug.put("userLoadedFromDB", false);
                        debug.put("note", "User not found in DB with email: " + email);
                    }
                }
            }
        } else {
            debug.put("userSource", "@AuthenticationPrincipal");
        }
        
        if (user == null) {
            debug.put("canRate", false);
            debug.put("reason", "Bạn chưa đăng nhập");
            debug.put("authInfo", request.getHeader("Authorization"));
            debug.put("sessionId", request.getSession(false) != null ? request.getSession(false).getId() : "no session");
            return ResponseEntity.ok(debug);
        }
        
        debug.put("userId", user.getId());
        debug.put("userEmail", user.getEmail());
        
        try {
            // Lấy thông tin đơn hàng
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            
            if (!orderOpt.isPresent()) {
                debug.put("canRate", false);
                debug.put("reason", "Không tìm thấy đơn hàng");
                return ResponseEntity.ok(debug);
            }
            
            Order order = orderOpt.get();
            debug.put("orderStatus", order.getStatus().toString());
            debug.put("orderUserId", order.getUser().getId());
            debug.put("orderBelongsToUser", order.getUser().getId().equals(user.getId()));
            debug.put("orderIsDelivered", order.getStatus() == OrderStatus.DELIVERED);
            
            boolean productInOrder = order.getOrderItems().stream()
                    .anyMatch(item -> item.getProduct().getId().equals(productId));
            debug.put("productInOrder", productInOrder);
            
            // Kiểm tra đã đánh giá chưa
            boolean canRate = ratingService.canUserRateProduct(user.getId(), productId, orderId);
            debug.put("canRate", canRate);
            
            // Xác định lý do không thể đánh giá
            if (!canRate) {
                if (!order.getUser().getId().equals(user.getId())) {
                    debug.put("reason", "Đơn hàng này không thuộc về bạn");
                } else if (order.getStatus() != OrderStatus.DELIVERED) {
                    String statusName = getStatusDisplayName(order.getStatus());
                    debug.put("reason", "Bạn chỉ có thể đánh giá sau khi đơn hàng đã được giao thành công. Trạng thái hiện tại: " + statusName);
                } else if (!productInOrder) {
                    debug.put("reason", "Sản phẩm này không có trong đơn hàng của bạn");
                } else {
                    debug.put("reason", "Bạn đã đánh giá sản phẩm này rồi");
                }
            }
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Lỗi debug quyền đánh giá", e);
            debug.put("canRate", false);
            debug.put("reason", "Có lỗi xảy ra: " + e.getMessage());
            debug.put("error", e.getClass().getSimpleName());
            return ResponseEntity.ok(debug);
        }
    }
    
    /**
     * Helper method để hiển thị tên trạng thái đơn hàng
     */
    private String getStatusDisplayName(OrderStatus status) {
        switch (status) {
            case PENDING: return "Chờ xác nhận";
            case CONFIRMED: return "Đã xác nhận";
            case SHIPPING: return "Đang giao hàng";
            case DELIVERED: return "Đã giao hàng";
            case CANCELLED: return "Đã hủy";
            default: return status.toString();
        }
    }
}
