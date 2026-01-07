package com.tulip.repository;

import com.tulip.entity.product.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    // Kiểm tra user đã đánh giá sản phẩm trong đơn hàng này chưa
    @Query("SELECT r FROM Rating r WHERE r.user.id = :userId AND r.product.id = :productId AND r.orderId = :orderId")
    Optional<Rating> findByUserAndProductAndOrder(@Param("userId") Long userId, 
                                                   @Param("productId") Long productId, 
                                                   @Param("orderId") Long orderId);
    
    // Lấy tất cả đánh giá của sản phẩm, sắp xếp theo utilityScore giảm dần, sau đó theo ngày tạo
    @Query("SELECT r FROM Rating r LEFT JOIN FETCH r.images WHERE r.product.id = :productId ORDER BY r.utilityScore DESC, r.createdAt DESC")
    List<Rating> findByProductIdOrderByUtilityScoreDesc(@Param("productId") Long productId);
    
    // Lấy đánh giá visible của sản phẩm (cho client)
    @Query("SELECT r FROM Rating r LEFT JOIN FETCH r.images WHERE r.product.id = :productId AND r.isVisible = :isVisible ORDER BY r.utilityScore DESC, r.createdAt DESC")
    List<Rating> findByProductIdAndIsVisibleOrderByUtilityScoreDesc(@Param("productId") Long productId, @Param("isVisible") Boolean isVisible);
    
    // Đếm số đánh giá theo sản phẩm
    long countByProductId(Long productId);
    
    // Tính điểm trung bình
    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.product.id = :productId")
    Double getAverageStarsByProductId(@Param("productId") Long productId);
    
    // Admin queries
    @Query("SELECT r FROM Rating r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user WHERE r.stars = :stars AND ((:hasReply = true AND r.adminReply IS NOT NULL) OR (:hasReply = false AND r.adminReply IS NULL)) ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<Rating> findByStarsAndHasReply(@Param("stars") Integer stars, @Param("hasReply") Boolean hasReply, org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT r FROM Rating r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user WHERE r.stars = :stars ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<Rating> findByStars(@Param("stars") Integer stars, org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT r FROM Rating r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user WHERE (:hasReply = true AND r.adminReply IS NOT NULL) OR (:hasReply = false AND r.adminReply IS NULL) ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<Rating> findByHasReply(@Param("hasReply") Boolean hasReply, org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT r FROM Rating r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<Rating> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
