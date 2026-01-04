package com.tulip.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO cho phiên live chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatSessionDTO {
    private Long id;
    private String sessionToken;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long staffId; // Null nếu chưa có staff nhận
    private String staffName;
    private String status; // NEW, PROCESSING, CLOSED
    private Long unreadCount; // Số tin nhắn chưa đọc
    private LocalDateTime lastMessageAt; // Thời gian tin nhắn cuối
    private String lastMessagePreview; // Nội dung tin nhắn cuối (preview)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

