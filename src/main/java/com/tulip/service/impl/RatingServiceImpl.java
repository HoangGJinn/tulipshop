package com.tulip.service.impl;

import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingSummaryDTO;
import com.tulip.entity.product.Rating;
import com.tulip.entity.product.RatingImage;
import com.tulip.repository.RatingRepository;
import com.tulip.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;

    // Lấy danh sách đánh giá của 1 sản phẩm
    public List<RatingDTO> getRatingsByProduct(Long productId) {
        List<Rating> ratings = ratingRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return ratings.stream().map(r -> RatingDTO.builder()
                .id(r.getId())
                .userName(r.getUser().getProfile() != null ? r.getUser().getProfile().getFullName() : r.getUser().getEmail())
                .userAvatar("https://placehold.co/50x50?text=" + r.getUser().getEmail().substring(0,1)) // Avatar giả lập
                .stars(r.getStars())
                .content(r.getContent())
                .variantInfo(r.getVariantInfo())
                .timeAgo(r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .images(r.getImages().stream().map(RatingImage::getImageUrl).collect(Collectors.toList()))
                .build()).collect(Collectors.toList());
    }

    // Tính toán tổng quan (Summary)
    public RatingSummaryDTO getRatingSummary(Long productId) {
        List<Rating> ratings = ratingRepository.findByProductIdOrderByCreatedAtDesc(productId);
        RatingSummaryDTO summary = new RatingSummaryDTO();

        int total = ratings.size();
        summary.setTotalReviews(total);

        if (total == 0) {
            summary.setAverageRating(0);
            summary.setCountPerStar(new HashMap<>());
            summary.setPercentPerStar(new HashMap<>());
            return summary;
        }

        // Tính trung bình
        double avg = ratings.stream().mapToInt(Rating::getStars).average().orElse(0);
        summary.setAverageRating(Math.round(avg * 10.0) / 10.0); // Làm tròn 1 số thập phân

        // Đếm số lượng từng sao
        Map<Integer, Integer> countMap = new HashMap<>();
        for(int i=5; i>=1; i--) countMap.put(i, 0); // Init

        for (Rating r : ratings) {
            countMap.put(r.getStars(), countMap.get(r.getStars()) + 1);
        }
        summary.setCountPerStar(countMap);

        // Tính phần trăm
        Map<Integer, Integer> percentMap = new HashMap<>();
        for(int i=5; i>=1; i--) {
            int percent = (int) (((double)countMap.get(i) / total) * 100);
            percentMap.put(i, percent);
        }
        summary.setPercentPerStar(percentMap);

        return summary;
    }
}