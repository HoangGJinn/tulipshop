package com.tulip.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponseDTO {
    private Long sessionId;
    private String sessionToken;
    private String status;
    private String customerName;
    private String customerEmail;
    private LocalDateTime createdAt;
}