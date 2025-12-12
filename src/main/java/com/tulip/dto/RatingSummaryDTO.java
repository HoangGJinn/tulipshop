package com.tulip.dto;

import lombok.Data;
import java.util.Map;

@Data
public class RatingSummaryDTO {
    private double averageRating;
    private int totalReviews;
    // Map lưu số lượng đánh giá cho mỗi sao. Key: số sao (5), Value: số lượng (100)
    private Map<Integer, Integer> countPerStar;
    // Map lưu phần trăm để vẽ thanh bar. Key: 5, Value: 80 (%)
    private Map<Integer, Integer> percentPerStar;
}