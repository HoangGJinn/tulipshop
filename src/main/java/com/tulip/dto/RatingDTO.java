package com.tulip.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RatingDTO {
    private Long id;
    private String userName;
    private String userAvatar;
    private int stars;
    private String content;
    private String variantInfo;
    private String timeAgo;
    private List<String> images;
}