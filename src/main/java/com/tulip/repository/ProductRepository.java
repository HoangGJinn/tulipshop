package com.tulip.repository;
import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.category c  " +
            "LEFT JOIN p.variants v " +
            "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword , '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', : keyword, '%')) " +
            "OR LOWER(v.colorName) LIKE LOWER(CONCAT('%', :keyword, '%'))"
    )
    List<Product> searchSmart(@Param("keyword") String keyword);

    List<Product> findTop5ByCategoryIdAndIdNot(Long categoryId, Long currentProductId);

    @Query("SELECT p FROM Product p " +
            "WHERE p.discountPrice IS NOT NULL " +
            "AND p.discountPrice > 0 " +
            "AND ((p.basePrice - p.discountPrice) * 1.0 / p.basePrice) >= 0.36")
    List<Product> findProductsWithDeepDiscount();

    // Hàng mới về (Lấy 5 sản phẩm có ID lớn nhất = mới nhất) - CHỈ ACTIVE
    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.id DESC LIMIT 5")
    List<Product> findTop5ByStatusOrderByIdDesc(@Param("status") ProductStatus status);

    // Sale > 18% - CHỈ ACTIVE
    @Query("SELECT p FROM Product p " +
            "WHERE p.status = :status " +
            "AND p.discountPrice > 0 " +
            "AND ((p.basePrice - p.discountPrice) / p.basePrice) > 0.18")
    List<Product> findProductsDiscountOver18(@Param("status") ProductStatus status);

    // Đang thịnh hành (Lấy 5 sản phẩm ngẫu nhiên) - CHỈ ACTIVE
    @Query(value = "SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY RAND() LIMIT 5", nativeQuery = true)
    List<Product> findRandomActiveProducts();

    // Tìm các sản phẩm mà cột tags có chứa từ khóa (ví dụ: "di-lam")
    List<Product> findByTagsContainingIgnoreCase(String tag);
    
    // Method mới: Lấy tất cả sản phẩm ACTIVE (cho admin)
    List<Product> findByStatus(ProductStatus status);
    
    // Method mới: Lấy tất cả sản phẩm ACTIVE hoặc HIDDEN (cho admin)
    List<Product> findByStatusIn(List<ProductStatus> statuses);

    // DEPRECATED methods - Giữ lại để tương thích ngược
    @Deprecated
    default List<Product> findTop5ByOrderByIdDesc() {
        return findTop5ByStatusOrderByIdDesc(ProductStatus.ACTIVE);
    }
    
    @Deprecated
    default List<Product> findProductsDiscountOver18() {
        return findProductsDiscountOver18(ProductStatus.ACTIVE);
    }
    
    @Deprecated
    default List<Product> findRandomProducts() {
        return findRandomActiveProducts();
    }
}