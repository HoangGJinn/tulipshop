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
    
    // Tìm sản phẩm theo danh sách category IDs (hỗ trợ N-cấp)
    @Query("SELECT p FROM Product p " +
           "WHERE p.category.id IN :categoryIds " +
           "AND p.status = :status")
    List<Product> findByCategoryIdInAndStatus(
        @Param("categoryIds") List<Long> categoryIds, 
        @Param("status") ProductStatus status
    );
    
    // Tìm sản phẩm có discount >= threshold (GIÁ TỐT)
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 'ACTIVE' " +
           "AND p.discountPrice IS NOT NULL " +
           "AND p.discountPrice > 0 " +
           "AND ((p.basePrice - p.discountPrice) * 100.0 / p.basePrice) >= :discountThreshold")
    List<Product> findProductsWithDiscountGreaterThan(@Param("discountThreshold") double discountThreshold);
    
    // Tìm sản phẩm bán chạy (SẢN PHẨM BÁN CHẠY)
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN OrderItem oi ON oi.stock.variant.product.id = p.id " +
           "WHERE p.status = 'ACTIVE' " +
           "GROUP BY p.id " +
           "ORDER BY COALESCE(SUM(oi.quantity), 0) DESC")
    List<Product> findBestSellingProducts();
    
    // DEPRECATED: Giữ lại để tương thích ngược
    @Deprecated
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 'ACTIVE' " +
           "AND (p.category.name = :rootCategoryName " +
           "OR p.category.parent.name = :rootCategoryName " +
           "OR p.category.parent.parent.name = :rootCategoryName)")
    List<Product> findByRootCategoryName(@Param("rootCategoryName") String rootCategoryName);

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
    
    // Lọc theo thuộc tính kỹ thuật
    @Query("SELECT DISTINCT p FROM Product p " +
           "WHERE p.status = 'ACTIVE' " +
           "AND (:neckline IS NULL OR p.neckline = :neckline) " +
           "AND (:material IS NULL OR p.material = :material) " +
           "AND (:sleeveType IS NULL OR p.sleeveType = :sleeveType) " +
           "AND (:brand IS NULL OR p.brand = :brand)")
    List<Product> findByTechnicalAttributes(
        @Param("neckline") String neckline,
        @Param("material") String material,
        @Param("sleeveType") String sleeveType,
        @Param("brand") String brand
    );
    
    // Lấy danh sách giá trị unique cho mỗi thuộc tính (để hiển thị trong filter)
    @Query("SELECT DISTINCT p.neckline FROM Product p WHERE p.neckline IS NOT NULL AND p.status = 'ACTIVE'")
    List<String> findDistinctNecklines();
    
    @Query("SELECT DISTINCT p.material FROM Product p WHERE p.material IS NOT NULL AND p.status = 'ACTIVE'")
    List<String> findDistinctMaterials();
    
    @Query("SELECT DISTINCT p.sleeveType FROM Product p WHERE p.sleeveType IS NOT NULL AND p.status = 'ACTIVE'")
    List<String> findDistinctSleeveTypes();
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.status = 'ACTIVE'")
    List<String> findDistinctBrands();
}
