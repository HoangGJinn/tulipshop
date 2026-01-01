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
}
