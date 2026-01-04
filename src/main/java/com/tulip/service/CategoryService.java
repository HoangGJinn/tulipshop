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
    
    /**
     * Tìm category theo ID
     * @param id ID của category
     * @return Optional<Category>
     */
    Optional<Category> findById(Long id);
    
    /**
     * Lưu hoặc cập nhật category
     * @param category Category cần lưu
     * @return Category đã lưu
     */
    Category save(Category category);
    
    /**
     * Xóa category theo ID
     * @param id ID của category cần xóa
     * @throws com.tulip.exception.BusinessException nếu category đang có sản phẩm
     */
    void deleteById(Long id);
    
    /**
     * Kiểm tra xem category có sản phẩm không
     * @param categoryId ID của category
     * @return true nếu có sản phẩm, false nếu không
     */
    boolean hasProducts(Long categoryId);
    
    /**
     * Tạo slug từ tên category
     * @param name Tên category
     * @return Slug đã chuẩn hóa
     */
    String generateSlug(String name);
}
