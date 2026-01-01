package com.tulip.repository;

import com.tulip.entity.product.ProductStock;
import com.tulip.entity.product.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    
    @Query("SELECT ps FROM ProductStock ps " +
           "JOIN FETCH ps.variant v " +
           "JOIN FETCH v.product p " +
           "JOIN FETCH ps.size s " +
           "LEFT JOIN FETCH v.images")
    List<ProductStock> findAllWithDetails();
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductStock ps WHERE ps.id = :id")
    Optional<ProductStock> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT v FROM ProductVariant v " +
           "WHERE v.id NOT IN (SELECT DISTINCT ps.variant.id FROM ProductStock ps)")
    List<ProductVariant> findVariantsWithoutStock();
    
    /**
     * Find all variant-size combinations that don't have stock records
     * This returns a list of Object arrays where:
     * [0] = ProductVariant
     * [1] = Size
     */
    @Query("SELECT v, s FROM ProductVariant v " +
           "CROSS JOIN Size s " +
           "WHERE NOT EXISTS (" +
           "  SELECT 1 FROM ProductStock ps " +
           "  WHERE ps.variant.id = v.id AND ps.size.id = s.id" +
           ")")
    List<Object[]> findUninitializedVariantSizeCombinations();
    
    /**
     * Tính tổng tồn kho của tất cả variants của một sản phẩm
     */
    @Query("SELECT COALESCE(SUM(ps.quantity), 0) FROM ProductStock ps " +
           "WHERE ps.variant.product.id = :productId")
    int sumQuantityByProductId(@Param("productId") Long productId);
}