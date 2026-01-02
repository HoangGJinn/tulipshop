package com.tulip.controller.admin;

import com.tulip.dto.RecentOrderDTO;
import com.tulip.dto.TopSellingProductDTO;
import com.tulip.dto.response.DashboardStatsDTO;
import com.tulip.entity.RevenueStats;
import com.tulip.service.DashboardService;
import com.tulip.service.RevenueStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API để quản lý và trigger tính toán revenue stats
 */
@RestController
@RequestMapping("/v1/api/admin/revenue-stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RevenueStatsController {
    
    private final RevenueStatsService revenueStatsService;
    private final DashboardService dashboardService;
    
    // POST /v1/api/admin/revenue-stats/refresh-today
    @PostMapping("/refresh-today")
    public ResponseEntity<DashboardStatsDTO> refreshTodayStats() {
        LocalDate today = LocalDate.now();
        revenueStatsService.calculateAndSaveDailyStats(today);
        
        // Trả về dashboard stats mới
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Lấy dashboard stats (không tính lại, chỉ đọc)
     * GET /v1/api/admin/revenue-stats/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
    
    // GET /v1/api/admin/revenue-stats/chart/week
    @GetMapping("/chart/week")
    public ResponseEntity<List<RevenueStats>> getWeeklyChartData() {
        List<RevenueStats> stats = revenueStatsService.getLast7Days();
        return ResponseEntity.ok(stats);
    }
    
    // GET /v1/api/admin/revenue-stats/chart/month
    @GetMapping("/chart/month")
    public ResponseEntity<List<RevenueStats>> getMonthlyChartData() {
        List<RevenueStats> stats = revenueStatsService.getLast30Days();
        return ResponseEntity.ok(stats);
    }
    
    // GET /v1/api/admin/revenue-stats/chart/quarter
    @GetMapping("/chart/quarter")
    public ResponseEntity<List<RevenueStats>> getQuarterlyChartData() {
        List<RevenueStats> stats = revenueStatsService.getLast3Months();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Lấy 5 đơn hàng mới nhất
     * GET /v1/api/admin/revenue-stats/recent-orders
     */
    @GetMapping("/recent-orders")
    public ResponseEntity<List<RecentOrderDTO>> getRecentOrders() {
        List<RecentOrderDTO> orders = dashboardService.getRecentOrders();
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Lấy top 5 sản phẩm bán chạy
     * GET /v1/api/admin/revenue-stats/top-selling
     */
    @GetMapping("/top-selling")
    public ResponseEntity<List<TopSellingProductDTO>> getTopSellingProducts() {
        List<TopSellingProductDTO> products = dashboardService.getTopSellingProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * Khởi tạo dữ liệu monthly và quarterly cho 12 tháng gần nhất
     * POST /v1/api/admin/revenue-stats/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeMonthlyAndQuarterlyData() {
        LocalDate today = LocalDate.now();
        int monthCount = 0;
        int quarterCount = 0;
        
        // Tạo dữ liệu cho 12 tháng gần nhất
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = today.minusMonths(i);
            revenueStatsService.calculateAndSaveMonthlyStats(monthDate);
            monthCount++;
        }
        
        // Tạo dữ liệu cho 4 quý gần nhất
        for (int i = 0; i < 4; i++) {
            LocalDate quarterDate = today.minusMonths(i * 3);
            revenueStatsService.calculateAndSaveQuarterlyStats(quarterDate);
            quarterCount++;
        }
        
        return ResponseEntity.ok(String.format(
            "Initialized %d monthly stats and %d quarterly stats", 
            monthCount, quarterCount
        ));
    }
    
    /**
     * Tính stats cho một ngày cụ thể
     * POST /api/admin/revenue-stats/daily?date=2026-01-01
     */
    @PostMapping("/daily")
    public ResponseEntity<RevenueStats> calculateDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RevenueStats stats = revenueStatsService.calculateAndSaveDailyStats(date);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Tính stats cho tuần
     * POST /api/admin/revenue-stats/weekly?endDate=2026-01-07
     */
    @PostMapping("/weekly")
    public ResponseEntity<RevenueStats> calculateWeeklyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        RevenueStats stats = revenueStatsService.calculateAndSaveWeeklyStats(endDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Tính stats cho tháng
     * POST /api/admin/revenue-stats/monthly?date=2026-01-15
     */
    @PostMapping("/monthly")
    public ResponseEntity<RevenueStats> calculateMonthlyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RevenueStats stats = revenueStatsService.calculateAndSaveMonthlyStats(date);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Tính stats cho quý
     * POST /api/admin/revenue-stats/quarterly?endDate=2026-03-31
     */
    @PostMapping("/quarterly")
    public ResponseEntity<RevenueStats> calculateQuarterlyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        RevenueStats stats = revenueStatsService.calculateAndSaveQuarterlyStats(endDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Tính stats cho nhiều ngày (batch)
     * POST /api/admin/revenue-stats/batch?startDate=2025-12-01&endDate=2025-12-31
     */
    @PostMapping("/batch")
    public ResponseEntity<String> calculateBatchStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        int count = 0;
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            revenueStatsService.calculateAndSaveDailyStats(currentDate);
            currentDate = currentDate.plusDays(1);
            count++;
        }
        
        return ResponseEntity.ok("Calculated stats for " + count + " days");
    }
}
