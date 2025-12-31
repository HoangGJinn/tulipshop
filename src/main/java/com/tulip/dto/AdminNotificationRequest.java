package com.tulip.dto;

import com.tulip.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để Admin tạo và gửi thông báo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    @NotNull(message = "Loại thông báo không được để trống")
    private Notification.NotificationType type;
    
    /**
     * Đối tượng nhận: ALL (tất cả) hoặc SPECIFIC (người dùng cụ thể)
     */
    @NotBlank(message = "Đối tượng nhận không được để trống")
    private String targetType; // ALL hoặc SPECIFIC
    
    /**
     * Email người nhận (bắt buộc nếu targetType = SPECIFIC)
     */
    private String recipientEmail;
    
    /**
     * Link điều hướng khi click vào thông báo
     */
    private String targetUrl;
    
    /**
     * Link ảnh minh họa (Cloudinary)
     */
    private String imageUrl;
}
