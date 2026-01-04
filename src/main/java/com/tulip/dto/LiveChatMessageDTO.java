package com.tulip.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO cho tin nhắn live chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatMessageDTO {
    private Long id;
    private Long sessionId;
    private String sessionToken;
    private String content;
    private String senderType; // CUSTOMER, SUPPORT_AGENT
    private Long senderId; // ID của người gửi (user hoặc staff)
    private String senderName; // Tên người gửi
    private Boolean isRead;
    private LocalDateTime timestamp;
}

