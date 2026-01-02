package com.tulip.service;

import com.tulip.dto.RecentOrderDTO;
import com.tulip.dto.TopSellingProductDTO;
import com.tulip.dto.response.DashboardStatsDTO;

import java.util.List;

public interface DashboardService {
    // Lấy thống kê tổng quan cho dashboard
    DashboardStatsDTO getDashboardStats();
    
    // Lấy 5 đơn hàng mới nhất
    
    List<RecentOrderDTO> getRecentOrders();
    
    // Lấy top 5 sản phẩm bán chạy nhất
    List<TopSellingProductDTO> getTopSellingProducts();
}
