package com.tulip.controller.admin;

import com.tulip.dto.CategoryStatsDTO;
import com.tulip.service.CategoryStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


// REST API cho thống kê phân loại sản phẩm
@RestController
@RequestMapping("/v1/api/admin/category-stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class CategoryStatsController {
    
    private final CategoryStatsService categoryStatsService;
    
    // Lấy thống kê số lượng sản phẩm theo category
    @GetMapping
    public ResponseEntity<List<CategoryStatsDTO>> getCategoryStats() {
        List<CategoryStatsDTO> stats = categoryStatsService.getCategoryStats();
        return ResponseEntity.ok(stats);
    }
}
