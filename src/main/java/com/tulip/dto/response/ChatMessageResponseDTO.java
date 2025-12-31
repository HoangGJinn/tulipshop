package com.tulip.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDTO {
    private Long id;
    private String content;
    private String senderType;
    private String messageType;
    private String aiResponse;
    private String policyAdvice;
    private LocalDateTime createdAt;
    private Boolean seen;
    private List<Long> recommendedProductIds; // Trả về list ID để Frontend dễ xử lý
}