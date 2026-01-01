package com.tulip.service.impl;

import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingRequest;
import com.tulip.dto.RatingStatistics;
import com.tulip.entity.Order;
import com.tulip.entity.OrderItem;
import com.tulip.entity.User;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.product.Product;
import com.tulip.entity.product.Rating;
import com.tulip.entity.product.RatingImage;
import com.tulip.repository.OrderRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.RatingRepository;
import com.tulip.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public RatingDTO submitRating(RatingRequest request, User user) {
        // 1. Validate: Kiểm tra quyền đánh giá
        if (!canUserRateProduct(user.getId(), request.getProductId(), request.getOrderId())) {
            throw new IllegalStateException("Bạn không có quyền đánh giá sản phẩm này hoặc đã đánh giá rồi");
        }

        // 2. Lấy thông tin Product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        // 3. Tính toán utilityScore
        double utilityScore = calculateUtilityScore(
                request.getContent(),
                request.getImages() != null ? request.getImages().size() : 0
        );

        // 4. Tạo Rating entity
        Rating rating = Rating.builder()
                .product(product)
                .user(user)
                .orderId(request.getOrderId())
                .stars(request.getStars())
                .content(request.getContent())
                .variantInfo(request.getVariantInfo())
                .utilityScore(utilityScore)
                .images(new ArrayList<>())
                .build();

        // 5. Upload và lưu hình ảnh lên Cloudinary
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (MultipartFile imageFile : request.getImages()) {
                if (!imageFile.isEmpty()) {
                    try {
                        // Upload lên Cloudinary
                        String imageUrl = cloudinaryService.uploadImage(imageFile);
                        RatingImage ratingImage = RatingImage.builder()
                                .rating(rating)
                                .imageUrl(imageUrl)
                                .build();
                        rating.getImages().add(ratingImage);
                        log.info("✅ Uploaded rating image to Cloudinary: {}", imageUrl);
                    } catch (Exception e) {
                        log.error("❌ Lỗi upload ảnh đánh giá lên Cloudinary: {}", e.getMessage());
                    }
                }
            }
        }

        // 6. Lưu vào database
        Rating savedRating = ratingRepository.save(rating);
        
        log.info("User {} đã đánh giá sản phẩm {} với utilityScore: {}", 
                user.getId(), product.getId(), utilityScore);

        // 7. Convert sang DTO
        return convertToDTO(savedRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingDTO> getProductRatings(Long productId) {
        List<Rating> ratings = ratingRepository.findByProductIdOrderByUtilityScoreDesc(productId);
        return ratings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserRateProduct(Long userId, Long productId, Long orderId) {
        // 1. Kiểm tra đơn hàng tồn tại và thuộc về user
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUser().getId().equals(userId)) {
            return false;
        }

        // 2. Kiểm tra đơn hàng đã hoàn thành
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return false;
        }

        // 3. Kiểm tra sản phẩm có trong đơn hàng
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!productInOrder) {
            return false;
        }

        // 4. Kiểm tra chưa đánh giá
        return ratingRepository.findByUserAndProductAndOrder(userId, productId, orderId).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public RatingStatistics getProductRatingStatistics(Long productId) {
        List<Rating> ratings = ratingRepository.findByProductIdOrderByUtilityScoreDesc(productId);
        
        long total = ratings.size();
        double average = total > 0 ? ratings.stream().mapToInt(Rating::getStars).average().orElse(0.0) : 0.0;
        
        long fiveStar = ratings.stream().filter(r -> r.getStars() == 5).count();
        long fourStar = ratings.stream().filter(r -> r.getStars() == 4).count();
        long threeStar = ratings.stream().filter(r -> r.getStars() == 3).count();
        long twoStar = ratings.stream().filter(r -> r.getStars() == 2).count();
        long oneStar = ratings.stream().filter(r -> r.getStars() == 1).count();
        
        return RatingStatistics.builder()
                .totalRatings(total)
                .averageStars(Math.round(average * 10.0) / 10.0)
                .fiveStars(fiveStar)
                .fourStars(fourStar)
                .threeStars(threeStar)
                .twoStars(twoStar)
                .oneStar(oneStar)
                .build();
    }

    /**
     * Thuật toán tính điểm hữu ích (Utility Score)
     * - Cơ bản: 0 điểm
     * - Số lượng từ: +1 điểm cho mỗi 10 từ (tối đa 20 điểm)
     * - Hình ảnh: +30 điểm cho ảnh đầu tiên, +10 điểm cho mỗi ảnh thêm (tối đa 50 điểm)
     * - Chất lượng: Loại bỏ spam/từ vô nghĩa
     */
    private double calculateUtilityScore(String content, int imageCount) {
        double score = 0.0;

        // 1. Điểm từ nội dung text
        if (content != null && !content.trim().isEmpty()) {
            String cleanContent = content.trim();
            
            // Kiểm tra spam đơn giản
            if (isSpamContent(cleanContent)) {
                return 0.0; // Spam = 0 điểm
            }
            
            // Đếm số từ
            int wordCount = cleanContent.split("\\s+").length;
            
            // +1 điểm cho mỗi 10 từ, tối đa 20 điểm
            double wordScore = Math.min((wordCount / 10.0), 20.0);
            score += wordScore;
        }

        // 2. Điểm từ hình ảnh
        if (imageCount > 0) {
            // Ảnh đầu tiên: +30 điểm
            score += 30.0;
            
            // Mỗi ảnh thêm: +10 điểm
            if (imageCount > 1) {
                score += Math.min((imageCount - 1) * 10.0, 20.0); // Tối đa thêm 20 điểm
            }
        }

        return Math.round(score * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân
    }

    /**
     * Kiểm tra nội dung spam đơn giản
     */
    private boolean isSpamContent(String content) {
        if (content.length() < 5) {
            return true; // Quá ngắn
        }
        
        // Kiểm tra lặp ký tự (ví dụ: "aaaaaaa", "111111")
        if (content.matches("(.)\\1{9,}")) {
            return true;
        }
        
        // Kiểm tra các từ spam phổ biến
        String lowerContent = content.toLowerCase();
        String[] spamKeywords = {"spam", "fake", "bot", "test test test"};
        for (String keyword : spamKeywords) {
            if (lowerContent.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Convert Rating entity sang DTO
     */
    private RatingDTO convertToDTO(Rating rating) {
        List<String> imageUrls = rating.getImages().stream()
                .map(RatingImage::getImageUrl)
                .collect(Collectors.toList());

        // Lấy thông tin user từ profile
        String userName = "Khách hàng";
        String userAvatar = null;
        
        if (rating.getUser() != null && rating.getUser().getProfile() != null) {
            userName = rating.getUser().getProfile().getFullName() != null ? 
                      rating.getUser().getProfile().getFullName() : "Khách hàng";
            userAvatar = rating.getUser().getProfile().getAvatar();
        }

        return RatingDTO.builder()
                .id(rating.getId())
                .productId(rating.getProduct().getId())
                .userId(rating.getUser().getId())
                .userName(userName)
                .userAvatar(userAvatar)
                .stars(rating.getStars())
                .content(rating.getContent())
                .variantInfo(rating.getVariantInfo())
                .utilityScore(rating.getUtilityScore())
                .createdAt(rating.getCreatedAt())
                .imageUrls(imageUrls)
                .isHighQuality(rating.getUtilityScore() >= 40.0)
                .build();
    }
}
