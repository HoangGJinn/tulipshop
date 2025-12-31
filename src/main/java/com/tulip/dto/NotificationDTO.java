package com.tulip.dto;

import com.tulip.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO để truyền thông tin thông báo qua API và WebSocket
 * Trường isRead được tính động dựa trên NotificationRead
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    
    private Long id;
    private String title;
    private String content;
    private String link;
    private String imageUrl;
    private String type; // ORDER, PROMOTION, SYSTEM
    private Boolean isRead; // Tính động từ NotificationRead
    private Boolean isBroadcast; // true nếu user = NULL
    private LocalDateTime createdAt;
    
    /**
     * Chuyển đổi từ Entity sang DTO (không có thông tin isRead)
     * Cần gọi setIsRead() sau khi tạo DTO
     */
    public static NotificationDTO fromEntity(Notification notification) {
        return NotificationDTO.builder()
            .id(notification.getId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .link(notification.getLink())
            .imageUrl(notification.getImageUrl())
            .type(notification.getType().name())
            .isRead(false) // Mặc định false, cần set lại sau
            .isBroadcast(notification.getUser() == null)
            .createdAt(notification.getCreatedAt())
            .build();
    }
    
    /**
     * Chuyển đổi từ Entity sang DTO với thông tin isRead
     */
    public static NotificationDTO fromEntity(Notification notification, boolean isRead) {
        NotificationDTO dto = fromEntity(notification);
        dto.setIsRead(isRead);
        return dto;
    }
}
