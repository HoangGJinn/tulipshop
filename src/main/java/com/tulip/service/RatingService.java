package com.tulip.service;

import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingRequest;
import com.tulip.dto.RatingStatistics;
import com.tulip.entity.User;

import java.util.List;

public interface RatingService {
    
    /**
     * Submit đánh giá sản phẩm với tính toán utilityScore thông minh
     */
    RatingDTO submitRating(RatingRequest request, User user);
    
    /**
     * Lấy danh sách đánh giá của sản phẩm (sắp xếp thông minh)
     */
    List<RatingDTO> getProductRatings(Long productId);
    
    /**
     * Kiểm tra user có thể đánh giá sản phẩm trong đơn hàng này không
     */
    boolean canUserRateProduct(Long userId, Long productId, Long orderId);
    
    /**
     * Lấy thống kê đánh giá của sản phẩm
     */
    RatingStatistics getProductRatingStatistics(Long productId);
    
    /**
     * Lấy thông tin đơn hàng (dùng cho debug)
     */
    com.tulip.entity.Order getOrderById(Long orderId);
    
    /**
     * Admin phản hồi đánh giá
     */
    RatingDTO replyToRating(Long ratingId, String replyContent);
    
    /**
     * Admin toggle hiển thị đánh giá
     */
    RatingDTO toggleVisibility(Long ratingId);
    
    /**
     * Lấy tất cả đánh giá cho Admin (bao gồm cả ẩn)
     */
    org.springframework.data.domain.Page<RatingDTO> getAllRatingsForAdmin(
        Integer stars, 
        Boolean hasReply, 
        org.springframework.data.domain.Pageable pageable
    );
}
