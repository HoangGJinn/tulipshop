package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingDTO {
    private Long id;
    private Long productId;
    private String productName; // Thêm để hiển thị trong admin
    private Long userId;
    private String userName;
    private String userAvatar;
    private Integer stars;
    private String content;
    private String variantInfo;
    private Double utilityScore;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private boolean isHighQuality; // utilityScore >= 40
    
    // Admin fields
    private String adminReply;
    private LocalDateTime replyTime;
    private Boolean isVisible;
}
