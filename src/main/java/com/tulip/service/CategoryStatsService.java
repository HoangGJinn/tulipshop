package com.tulip.service;

import com.tulip.dto.CategoryStatsDTO;

import java.util.List;

public interface CategoryStatsService {
    
    // Lấy thống kê số lượng sản phẩm theo category
    List<CategoryStatsDTO> getCategoryStats();
}
