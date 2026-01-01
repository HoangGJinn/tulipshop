package com.tulip.repository;

import com.tulip.entity.product.ProductAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductAuditRepository extends JpaRepository<ProductAudit, Long> {
    List<ProductAudit> findByProductIdOrderByChangedAtDesc(Long productId);
    List<ProductAudit> findByChangedByOrderByChangedAtDesc(String changedBy);
}
