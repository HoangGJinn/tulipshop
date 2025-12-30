package com.tulip.service;

import com.tulip.dto.NotificationDTO;
import com.tulip.dto.NotificationRequest;
import com.tulip.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface để quản lý thông báo
 */
public interface NotificationService {
    
    /**
     * Tạo và gửi thông báo đến user qua WebSocket
     * @param username Email của user
     * @param request Thông tin thông báo
     * @return Thông báo đã tạo
     */
    NotificationDTO sendNotification(String username, NotificationRequest request);
    
    /**
     * Lấy danh sách thông báo của user hiện tại
     */
    Page<NotificationDTO> getNotifications(Pageable pageable);
    
    /**
     * Lấy danh sách thông báo theo loại
     */
    Page<NotificationDTO> getNotificationsByType(Notification.NotificationType type, Pageable pageable);
    
    /**
     * Đếm số thông báo chưa đọc
     */
    Long countUnreadNotifications();
    
    /**
     * Đếm số thông báo chưa đọc theo loại
     */
    Long countUnreadNotificationsByType(Notification.NotificationType type);
    
    /**
     * Đánh dấu thông báo là đã đọc
     */
    void markAsRead(Long notificationId);
    
    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    void markAllAsRead();
    
    /**
     * Đánh dấu tất cả thông báo theo loại là đã đọc
     */
    void markAllAsReadByType(Notification.NotificationType type);
    
    /**
     * Xóa thông báo cũ (chạy định kỳ)
     */
    void deleteOldNotifications(int daysToKeep);
    
    /**
     * Gửi thông báo broadcast (cho tất cả người dùng)
     * Chỉ lưu 1 bản ghi với user = NULL
     */
    NotificationDTO sendBroadcastNotification(NotificationRequest request);
}
