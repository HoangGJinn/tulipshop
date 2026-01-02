package com.tulip.repository;
import com.tulip.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    
    // Đếm số lượng sản phẩm (không bị xóa) theo từng category
    @Query("SELECT c.id as categoryId, c.name as categoryName, COUNT(p.id) as productCount " +
           "FROM Category c " +
           "LEFT JOIN Product p ON p.category.id = c.id AND p.status != 'DELETED' " +
           "GROUP BY c.id, c.name " +
           "ORDER BY COUNT(p.id) DESC")
    List<Object[]> getCategoryStats();


}