package com.tulip.service;

import com.tulip.entity.product.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    /**
     * Lấy tất cả ID của category và các category con (đệ quy N-cấp)
     * @param categoryId ID của category gốc
     * @return Danh sách ID bao gồm chính nó và tất cả con cháu
     */
    List<Long> getAllChildCategoryIds(Long categoryId);
    
    /**
     * Tìm category theo slug
     * @param slug Slug của category
     * @return Optional<Category>
     */
    Optional<Category> findBySlug(String slug);
    
    /**
     * Lấy tất cả categories
     * @return Danh sách tất cả categories
     */
    List<Category> findAll();
}
