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
import java.util.Optional;
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
        // 1. Validate: Kiểm tra quyền đánh giá với message lỗi chi tiết
        Order order = orderRepository.findById(request.getOrderId()).orElse(null);
        
        if (order == null) {
            throw new IllegalStateException("Không tìm thấy đơn hàng");
        }
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Đơn hàng này không thuộc về bạn");
        }
        
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                "Bạn chỉ có thể đánh giá sau khi đơn hàng đã được giao thành công. " +
                "Trạng thái hiện tại: " + getStatusDisplayName(order.getStatus())
            );
        }
        
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));
        if (!productInOrder) {
            throw new IllegalStateException("Sản phẩm này không có trong đơn hàng của bạn");
        }
        
        Optional<Rating> existingRating = ratingRepository.findByUserAndProductAndOrder(
            user.getId(), request.getProductId(), request.getOrderId());
        if (existingRating.isPresent()) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi");
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
        // Chỉ lấy các đánh giá có isVisible = true cho client
        List<Rating> ratings = ratingRepository.findByProductIdAndIsVisibleOrderByUtilityScoreDesc(productId, true);
        return ratings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserRateProduct(Long userId, Long productId, Long orderId) {
        log.info("🔍 Checking rating permission: userId={}, productId={}, orderId={}", 
                 userId, productId, orderId);
        
        // 1. Kiểm tra đơn hàng tồn tại và thuộc về user
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("❌ Order not found: orderId={}", orderId);
            return false;
        }
        if (!order.getUser().getId().equals(userId)) {
            log.warn("❌ Order does not belong to user: orderId={}, userId={}, orderUserId={}", 
                     orderId, userId, order.getUser().getId());
            return false;
        }

        // 2. Kiểm tra đơn hàng đã hoàn thành
        if (order.getStatus() != OrderStatus.DELIVERED) {
            log.warn("❌ Order not delivered yet: orderId={}, status={}", orderId, order.getStatus());
            return false;
        }

        // 3. Kiểm tra sản phẩm có trong đơn hàng
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!productInOrder) {
            log.warn("❌ Product not in order: productId={}, orderId={}", productId, orderId);
            return false;
        }

        // 4. Kiểm tra chưa đánh giá
        Optional<Rating> existingRating = ratingRepository.findByUserAndProductAndOrder(userId, productId, orderId);
        if (existingRating.isPresent()) {
            log.warn("❌ User already rated this product: userId={}, productId={}, orderId={}", 
                     userId, productId, orderId);
            return false;
        }
        
        log.info("✅ User can rate product: userId={}, productId={}, orderId={}", 
                 userId, productId, orderId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public RatingStatistics getProductRatingStatistics(Long productId) {
        // Chỉ tính thống kê từ các đánh giá visible
        List<Rating> ratings = ratingRepository.findByProductIdAndIsVisibleOrderByUtilityScoreDesc(productId, true);
        
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
    
    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
    
    @Override
    @Transactional
    public RatingDTO replyToRating(Long ratingId, String replyContent) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        
        rating.setAdminReply(replyContent);
        rating.setReplyTime(java.time.LocalDateTime.now());
        
        Rating savedRating = ratingRepository.save(rating);
        log.info("Admin đã phản hồi đánh giá ID: {}", ratingId);
        
        return convertToDTO(savedRating);
    }
    
    @Override
    @Transactional
    public RatingDTO toggleVisibility(Long ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        
        rating.setIsVisible(!rating.getIsVisible());
        Rating savedRating = ratingRepository.save(rating);
        
        log.info("Admin đã {} đánh giá ID: {}", 
                savedRating.getIsVisible() ? "hiện" : "ẩn", ratingId);
        
        return convertToDTO(savedRating);
    }
    
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RatingDTO> getAllRatingsForAdmin(
            Integer stars, Boolean hasReply, org.springframework.data.domain.Pageable pageable) {
        
        org.springframework.data.domain.Page<Rating> ratingsPage;
        
        if (stars != null && hasReply != null) {
            ratingsPage = ratingRepository.findByStarsAndHasReply(stars, hasReply, pageable);
        } else if (stars != null) {
            ratingsPage = ratingRepository.findByStars(stars, pageable);
        } else if (hasReply != null) {
            ratingsPage = ratingRepository.findByHasReply(hasReply, pageable);
        } else {
            ratingsPage = ratingRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        return ratingsPage.map(this::convertToDTO);
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
                .productName(rating.getProduct().getName())
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
                .adminReply(rating.getAdminReply())
                .replyTime(rating.getReplyTime())
                .isVisible(rating.getIsVisible())
                .build();
    }
    
    /**
     * Helper method để hiển thị tên trạng thái đơn hàng
     */
    private String getStatusDisplayName(OrderStatus status) {
        switch (status) {
            case PENDING: return "Chờ xác nhận";
            case CONFIRMED: return "Đã xác nhận";
            case SHIPPING: return "Đang giao hàng";
            case DELIVERED: return "Đã giao hàng";
            case CANCELLED: return "Đã hủy";
            default: return status.toString();
        }
    }
}
