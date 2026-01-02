package com.tulip.service.impl;

import com.tulip.dto.CategoryStatsDTO;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.CategoryStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation của CategoryStatsService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryStatsServiceImpl implements CategoryStatsService {
    
    private final CategoryRepository categoryRepository;
    
    // Màu sắc cho biểu đồ (đen, xám đậm, xám nhạt, xám sáng, xanh, đỏ, vàng, tím)
    private static final String[] COLORS = {
        "#000000", "#4b5563", "#6b7280", "#9ca3af", 
        "#3b82f6", "#ef4444", "#f59e0b", "#8b5cf6"
    };
    
    @Override
    public List<CategoryStatsDTO> getCategoryStats() {
        List<Object[]> rawData = categoryRepository.getCategoryStats();
        
        // Tính tổng số sản phẩm
        long totalProducts = rawData.stream()
                .mapToLong(row -> ((Number) row[2]).longValue())
                .sum();
        
        // Tạo list kết quả với phần trăm và màu sắc
        List<CategoryStatsDTO> result = new ArrayList<>();
        
        for (int i = 0; i < rawData.size(); i++) {
            Object[] row = rawData.get(i);
            
            Long categoryId = ((Number) row[0]).longValue();
            String categoryName = (String) row[1];
            Long productCount = ((Number) row[2]).longValue();
            
            // Tính phần trăm
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalProducts > 0) {
                percentage = BigDecimal.valueOf(productCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalProducts), 2, RoundingMode.HALF_UP);
            }
            
            // Chọn màu (lặp lại nếu có nhiều hơn số màu định nghĩa)
            String color = COLORS[i % COLORS.length];
            
            CategoryStatsDTO dto = CategoryStatsDTO.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .productCount(productCount)
                    .percentage(percentage)
                    .color(color)
                    .build();
            
            result.add(dto);
        }
        
        return result;
    }
}
