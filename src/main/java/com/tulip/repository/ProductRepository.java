package com.tulip.repository;
import com.tulip.entity.product.Product;
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
    

}