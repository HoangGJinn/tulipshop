package com.tulip.service.impl;

import com.tulip.entity.product.Category;
import com.tulip.entity.product.ProductStatus;
import com.tulip.exception.BusinessException;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    
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
    
    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "categoryHierarchy", allEntries = true)
    public Category save(Category category) {
        // Tự động tạo slug nếu chưa có hoặc rỗng
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            category.setSlug(generateSlug(category.getName()));
        }
        
        // Đảm bảo slug là duy nhất
        String originalSlug = category.getSlug();
        String uniqueSlug = originalSlug;
        int counter = 1;
        
        while (true) {
            Optional<Category> existing = categoryRepository.findBySlug(uniqueSlug);
            // Nếu không tìm thấy hoặc tìm thấy chính nó (trường hợp update)
            if (existing.isEmpty() || 
                (category.getId() != null && existing.get().getId().equals(category.getId()))) {
                break;
            }
            uniqueSlug = originalSlug + "-" + counter;
            counter++;
        }
        
        category.setSlug(uniqueSlug);
        
        log.info("Saving category: {} with slug: {}", category.getName(), category.getSlug());
        return categoryRepository.save(category);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "categoryHierarchy", allEntries = true)
    public void deleteById(Long id) {
        // Kiểm tra xem category có tồn tại không
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy danh mục với ID: " + id));
        
        // Kiểm tra xem category có sản phẩm không
        if (hasProducts(id)) {
            throw new BusinessException("Không thể xóa danh mục đang có sản phẩm. Vui lòng chuyển sản phẩm sang danh mục khác trước.");
        }
        
        // Kiểm tra xem category có danh mục con không
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new BusinessException("Không thể xóa danh mục đang có danh mục con. Vui lòng xóa danh mục con trước.");
        }
        
        log.info("Deleting category: {} (ID: {})", category.getName(), id);
        categoryRepository.deleteById(id);
    }
    
    @Override
    public boolean hasProducts(Long categoryId) {
        // Lấy tất cả category IDs bao gồm cả con cháu
        List<Long> allCategoryIds = getAllChildCategoryIds(categoryId);
        
        // Kiểm tra xem có sản phẩm ACTIVE hoặc HIDDEN nào thuộc các category này không
        List<ProductStatus> activeStatuses = List.of(ProductStatus.ACTIVE, ProductStatus.HIDDEN);
        long productCount = productRepository.findByStatusIn(activeStatuses).stream()
            .filter(p -> p.getCategory() != null && allCategoryIds.contains(p.getCategory().getId()))
            .count();
        
        return productCount > 0;
    }
    
    @Override
    public String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        // Chuyển về chữ thường
        String slug = name.toLowerCase().trim();
        
        // Loại bỏ dấu tiếng Việt
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        slug = pattern.matcher(slug).replaceAll("");
        
        // Thay thế các ký tự đặc biệt
        slug = slug.replaceAll("đ", "d");
        slug = slug.replaceAll("Đ", "d");
        
        // Chỉ giữ lại chữ cái, số và dấu gạch ngang
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        
        // Thay thế khoảng trắng bằng dấu gạch ngang
        slug = slug.replaceAll("\\s+", "-");
        
        // Loại bỏ các dấu gạch ngang liên tiếp
        slug = slug.replaceAll("-+", "-");
        
        // Loại bỏ dấu gạch ngang ở đầu và cuối
        slug = slug.replaceAll("^-|-$", "");
        
        return slug;
    }
}
