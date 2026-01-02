package com.tulip.service.impl;

import com.tulip.entity.product.Category;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    /**
     * Lấy tất cả ID của category và các category con (đệ quy N-cấp)
     * Sử dụng cache để tối ưu performance
     */
    @Override
    @Cacheable(value = "categoryHierarchy", key = "#categoryId")
    public List<Long> getAllChildCategoryIds(Long categoryId) {
        List<Long> result = new ArrayList<>();
        collectChildIds(categoryId, result);
        log.debug("Collected {} category IDs for parent ID: {}", result.size(), categoryId);
        return result;
    }
    
    /**
     * Hàm đệ quy để thu thập tất cả ID con
     */
    private void collectChildIds(Long categoryId, List<Long> result) {
        // Thêm chính ID này vào kết quả
        result.add(categoryId);
        
        // Tìm category hiện tại
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return;
        }
        
        Category category = categoryOpt.get();
        
        // Nếu có children, đệ quy vào từng child
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            for (Category child : category.getChildren()) {
                collectChildIds(child.getId(), result);
            }
        }
    }
    
    @Override
    public Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }
    
    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
}
