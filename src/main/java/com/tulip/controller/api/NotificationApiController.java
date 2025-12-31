package com.tulip.controller.api;

import com.tulip.dto.NotificationDTO;
import com.tulip.entity.Notification;
import com.tulip.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/v1/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {
    
    private final NotificationService notificationService;
    
    /**
     * Lấy danh sách thông báo của user hiện tại
     * GET /v1/api/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getNotifications(pageable);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy danh sách thông báo theo loại
     * GET /v1/api/notifications/type/ORDER?page=0&size=20
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<NotificationDTO>> getNotificationsByType(
        @PathVariable String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Notification.NotificationType notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDTO> notifications = notificationService.getNotificationsByType(notificationType, pageable);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Đếm số thông báo chưa đọc
     * GET /v1/api/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Object>> countUnreadNotifications() {
        Long totalUnread = notificationService.countUnreadNotifications();
        Long orderUnread = notificationService.countUnreadNotificationsByType(Notification.NotificationType.ORDER);
        Long promotionUnread = notificationService.countUnreadNotificationsByType(Notification.NotificationType.PROMOTION);
        Long systemUnread = notificationService.countUnreadNotificationsByType(Notification.NotificationType.SYSTEM);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", totalUnread);
        result.put("order", orderUnread);
        result.put("promotion", promotionUnread);
        result.put("system", systemUnread);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Đánh dấu thông báo là đã đọc
     * PUT /v1/api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã đánh dấu thông báo là đã đọc");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Đánh dấu tất cả thông báo là đã đọc
     * PUT /v1/api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        try {
            notificationService.markAllAsRead();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã đánh dấu tất cả thông báo là đã đọc");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Đánh dấu tất cả thông báo theo loại là đã đọc
     * PUT /v1/api/notifications/read-all/type/{type}
     */
    @PutMapping("/read-all/type/{type}")
    public ResponseEntity<Map<String, String>> markAllAsReadByType(@PathVariable String type) {
        try {
            Notification.NotificationType notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
            notificationService.markAllAsReadByType(notificationType);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã đánh dấu tất cả thông báo loại " + type + " là đã đọc");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Loại thông báo không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error marking notifications as read: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
