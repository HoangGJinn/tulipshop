package com.tulip.dto;

import com.tulip.entity.enums.ChatRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerAvatar;
    private Long staffId;
    private String staffName;
    private String staffAvatar;
    private ChatRoomStatus status;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
    private LocalDateTime createdAt;
}

