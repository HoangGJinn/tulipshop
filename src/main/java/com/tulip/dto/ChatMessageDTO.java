package com.tulip.dto;

import com.tulip.entity.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private MessageType type;
    private String content;
    private Boolean seen;
    private LocalDateTime seenAt;
    private LocalDateTime createdAt;
}

