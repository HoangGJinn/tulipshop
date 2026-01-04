package com.tulip.repository;

import com.tulip.entity.product.ProductAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductAuditRepository extends JpaRepository<ProductAudit, Long> {
    List<ProductAudit> findByProductIdOrderByChangedAtDesc(Long productId);
    List<ProductAudit> findByChangedByOrderByChangedAtDesc(String changedBy);
    
    /**
     * Lấy danh sách tất cả product_id đã được chỉnh sửa (không trùng lặp)
     * Sắp xếp sẽ được thực hiện ở service layer
     */
    @Query("SELECT DISTINCT pa.productId FROM ProductAudit pa")
    List<Long> findAllEditedProductIds();
    
    /**
     * Đếm số lần chỉnh sửa của một sản phẩm
     */
    @Query("SELECT COUNT(pa) FROM ProductAudit pa WHERE pa.productId = :productId")
    Integer countByProductId(Long productId);
}
