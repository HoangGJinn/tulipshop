package com.tulip.controller;

import com.tulip.dto.NotificationRequest;
import com.tulip.entity.Notification;
import com.tulip.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/test/notifications")
@RequiredArgsConstructor
public class NotificationTestController {
    
    private final NotificationService notificationService;
    
    /**
     * Test gửi thông báo đơn hàng
     * POST /test/notifications/order?email=user@example.com
     */
    @PostMapping("/order")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ admin mới được test
    public ResponseEntity<Map<String, String>> testOrderNotification(@RequestParam String email) {
        try {
            NotificationRequest request = NotificationRequest.builder()
                .title("🎉 Đặt hàng thành công")
                .content("Đơn hàng #TEST123 đã được đặt thành công. Tổng giá trị: 500,000 VNĐ")
                .link("/orders/123")
                .type(Notification.NotificationType.ORDER)
                .build();
            
            notificationService.sendNotification(email, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã gửi thông báo đơn hàng đến " + email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending test notification", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Test gửi thông báo khuyến mãi
     * POST /test/notifications/promotion?email=user@example.com
     */
    @PostMapping("/promotion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testPromotionNotification(@RequestParam String email) {
        try {
            NotificationRequest request = NotificationRequest.builder()
                .title("🎁 Khuyến mãi đặc biệt")
                .content("Giảm giá 50% cho tất cả sản phẩm mùa đông. Áp dụng từ hôm nay!")
                .link("/products?sale=true")
                .type(Notification.NotificationType.PROMOTION)
                .build();
            
            notificationService.sendNotification(email, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã gửi thông báo khuyến mãi đến " + email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending test notification", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Test gửi thông báo hệ thống
     * POST /test/notifications/system?email=user@example.com
     */
    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testSystemNotification(@RequestParam String email) {
        try {
            NotificationRequest request = NotificationRequest.builder()
                .title("⚙️ Thông báo hệ thống")
                .content("Hệ thống sẽ bảo trì từ 2:00 AM đến 4:00 AM ngày mai. Vui lòng hoàn tất giao dịch trước thời gian này.")
                .link("/")
                .type(Notification.NotificationType.SYSTEM)
                .build();
            
            notificationService.sendNotification(email, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã gửi thông báo hệ thống đến " + email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending test notification", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Test gửi thông báo tùy chỉnh
     * POST /test/notifications/custom
     * Body: {
     *   "email": "user@example.com",
     *   "title": "Test",
     *   "content": "Test content",
     *   "link": "/",
     *   "type": "ORDER"
     * }
     */
    @PostMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testCustomNotification(
        @RequestParam String email,
        @RequestBody NotificationRequest request
    ) {
        try {
            notificationService.sendNotification(email, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã gửi thông báo tùy chỉnh đến " + email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending test notification", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Test gửi nhiều thông báo liên tiếp
     * POST /test/notifications/bulk?email=user@example.com&count=5
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testBulkNotifications(
        @RequestParam String email,
        @RequestParam(defaultValue = "5") int count
    ) {
        try {
            for (int i = 1; i <= count; i++) {
                NotificationRequest request = NotificationRequest.builder()
                    .title("Thông báo test #" + i)
                    .content("Đây là thông báo test số " + i + " để kiểm tra hiển thị nhiều thông báo")
                    .link("/")
                    .type(i % 3 == 0 ? Notification.NotificationType.SYSTEM : 
                          i % 3 == 1 ? Notification.NotificationType.ORDER : 
                          Notification.NotificationType.PROMOTION)
                    .build();
                
                notificationService.sendNotification(email, request);
                
                // Delay nhỏ giữa các thông báo
                Thread.sleep(500);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã gửi " + count + " thông báo đến " + email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending bulk notifications", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
