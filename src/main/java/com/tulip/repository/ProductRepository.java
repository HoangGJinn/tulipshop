package com.tulip.repository;
import com.tulip.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    // Hàng mới về (Lấy 5 sản phẩm có ID lớn nhất = mới nhất)
    List<Product> findTop5ByOrderByIdDesc();

    // Sale > 18% (Lấy 10 sản phẩm, dùng Native Query để tính toán và giới hạn nhanh)
    @Query(value = "SELECT * FROM products p " +
            "WHERE p.discount_price > 0 " +
            "AND ((p.base_price - p.discount_price) / p.base_price) > 0.18 LIMIT 10", nativeQuery = true)
    List<Product> findProductsDiscountOver18();

    // Đang thịnh hành (Lấy 5 sản phẩm ngẫu nhiên)
    @Query(value = "SELECT * FROM products ORDER BY RAND() LIMIT 5", nativeQuery = true)
    List<Product> findRandomProducts();

    // Tìm các sản phẩm mà cột tags có chứa từ khóa (ví dụ: "di-lam")
    List<Product> findByTagsContainingIgnoreCase(String tag);
}