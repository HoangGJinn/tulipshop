package com.tulip.dto;

import com.tulip.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để tạo thông báo mới
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    private String link;
    
    private String imageUrl;
    
    @NotNull(message = "Loại thông báo không được để trống")
    private Notification.NotificationType type;
}
