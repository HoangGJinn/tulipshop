package com.tulip.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    // Doanh thu hôm nay
    private BigDecimal todayRevenue;
    private Double todayGrowthPercent; // % so với hôm qua
    
    // Tăng trưởng
    private Integer newOrders; // Đơn hàng mới hôm nay
    private Double ordersGrowthPercent; // % so với hôm qua
    private Integer newCustomers; // Khách mới hôm nay
    private Double customersGrowthPercent; // % so với hôm qua
    
    // Trạng thái đơn hàng
    private Integer pendingOrders; // Đang chờ xử lý
    private Integer confirmedOrders; // Đã xác nhận
    private Integer shippingOrders; // Đang giao hàng
    
    // Sắp hết hàng
    private Integer lowStockCount; // Số sản phẩm sắp hết
}
