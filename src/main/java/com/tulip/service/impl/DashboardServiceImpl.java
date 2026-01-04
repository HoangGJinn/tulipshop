package com.tulip.service.impl;

import com.tulip.dto.RecentOrderDTO;
import com.tulip.dto.TopSellingProductDTO;
import com.tulip.dto.response.DashboardStatsDTO;
import com.tulip.entity.Order;
import com.tulip.entity.RevenueStats;
import com.tulip.entity.User;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.product.ProductStock;
import com.tulip.repository.OrderRepository;
import com.tulip.repository.ProductStockRepository;
import com.tulip.repository.RevenueStatsRepository;
import com.tulip.repository.UserRepository;
import com.tulip.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductStockRepository productStockRepository;
    private final RevenueStatsRepository revenueStatsRepository;
    
    private static final int LOW_STOCK_THRESHOLD = 10; // Ngưỡng cảnh báo hết hàng
    
    @Override
    public DashboardStatsDTO getDashboardStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            
            // 1. LẤY STATS HÔM NAY TỪ BẢNG (nếu có)
            RevenueStats todayStats = revenueStatsRepository
                    .findByStatsDateAndStatsType(today, RevenueStats.StatsType.DAILY)
                    .orElse(null);
            
            BigDecimal todayRevenue = BigDecimal.ZERO;
            BigDecimal todayShippingFee = BigDecimal.ZERO;
            int newOrdersToday = 0;
            int newCustomersToday = 0;
            
            if (todayStats != null) {
                // Đọc từ bảng stats (đã được tính sẵn)
                todayRevenue = todayStats.getRevenue();
                todayShippingFee = todayStats.getShippingFee() != null ? todayStats.getShippingFee() : BigDecimal.ZERO;
                newOrdersToday = todayStats.getOrderCount();
                newCustomersToday = todayStats.getCustomerCount();
            } else {
                // Nếu chưa có trong bảng, tính real-time (fallback)
                LocalDateTime todayStart = today.atStartOfDay();
                LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
                List<Order> todayOrders = orderRepository.findByDateRange(todayStart, todayEnd);
                todayRevenue = calculateRevenue(todayOrders != null ? todayOrders : List.of());
                todayShippingFee = calculateShippingFee(todayOrders != null ? todayOrders : List.of());
                newOrdersToday = todayOrders != null ? todayOrders.size() : 0;
                
                List<User> allUsers = userRepository.findAll();
                newCustomersToday = (int) allUsers.stream()
                        .filter(user -> user.getCreatedAt() != null && 
                                user.getCreatedAt().isAfter(todayStart) && 
                                user.getCreatedAt().isBefore(todayEnd))
                        .count();
            }
            
            // 2. LẤY STATS HÔM QUA ĐỂ TÍNH % TĂNG TRƯỞNG
            RevenueStats yesterdayStats = revenueStatsRepository
                    .findByStatsDateAndStatsType(yesterday, RevenueStats.StatsType.DAILY)
                    .orElse(null);
            
            BigDecimal yesterdayRevenue = BigDecimal.ZERO;
            int newOrdersYesterday = 0;
            int newCustomersYesterday = 0;
            
            if (yesterdayStats != null) {
                yesterdayRevenue = yesterdayStats.getRevenue();
                newOrdersYesterday = yesterdayStats.getOrderCount();
                newCustomersYesterday = yesterdayStats.getCustomerCount();
            } else {
                // Fallback: tính từ orders
                LocalDateTime yesterdayStart = yesterday.atStartOfDay();
                LocalDateTime yesterdayEnd = yesterday.atTime(LocalTime.MAX);
                List<Order> yesterdayOrders = orderRepository.findByDateRange(yesterdayStart, yesterdayEnd);
                yesterdayRevenue = calculateRevenue(yesterdayOrders != null ? yesterdayOrders : List.of());
                newOrdersYesterday = yesterdayOrders != null ? yesterdayOrders.size() : 0;
            }
            
            Double todayGrowthPercent = calculateGrowthPercent(todayRevenue, yesterdayRevenue);
            Double ordersGrowthPercent = calculateGrowthPercent(newOrdersToday, newOrdersYesterday);
            Double customersGrowthPercent = calculateGrowthPercent(newCustomersToday, newCustomersYesterday);
            
            // 3. TRẠNG THÁI ĐƠN HÀNG - Real-time từ orders
            List<Order> pendingOrdersList = orderRepository.findByStatus(OrderStatus.PENDING);
            List<Order> confirmedOrdersList = orderRepository.findByStatus(OrderStatus.CONFIRMED);
            List<Order> shippingOrdersList = orderRepository.findByStatus(OrderStatus.SHIPPING);
            int pendingOrders = pendingOrdersList != null ? pendingOrdersList.size() : 0;
            int confirmedOrders = confirmedOrdersList != null ? confirmedOrdersList.size() : 0;
            int shippingOrders = shippingOrdersList != null ? shippingOrdersList.size() : 0;
            
            // 4. SẢN PHẨM SẮP HẾT HÀNG - Real-time từ stocks
            int lowStockCount = 0;
            
            try {
                List<ProductStock> allStocks = productStockRepository.findAllValidStocks();
                
                if (allStocks != null && !allStocks.isEmpty()) {
                    lowStockCount = (int) allStocks.stream()
                            .filter(stock -> stock.getQuantity() != null && 
                                    stock.getQuantity() > 0 && 
                                    stock.getQuantity() <= LOW_STOCK_THRESHOLD)
                            .count();
                }
            } catch (Exception e) {
                log.error("Error fetching product stocks: {}", e.getMessage());
                lowStockCount = 0;
            }
            
            return DashboardStatsDTO.builder()
                    .todayRevenue(todayRevenue)
                    .todayShippingFee(todayShippingFee)
                    .todayGrowthPercent(todayGrowthPercent)
                    .newOrders(newOrdersToday)
                    .ordersGrowthPercent(ordersGrowthPercent)
                    .newCustomers(newCustomersToday)
                    .customersGrowthPercent(customersGrowthPercent)
                    .pendingOrders(pendingOrders)
                    .confirmedOrders(confirmedOrders)
                    .shippingOrders(shippingOrders)
                    .lowStockCount(lowStockCount)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating dashboard stats", e);
            // Return default values in case of error
            return DashboardStatsDTO.builder()
                    .todayRevenue(BigDecimal.ZERO)
                    .todayShippingFee(BigDecimal.ZERO)
                    .todayGrowthPercent(0.0)
                    .newOrders(0)
                    .ordersGrowthPercent(0.0)
                    .newCustomers(0)
                    .customersGrowthPercent(0.0)
                    .pendingOrders(0)
                    .confirmedOrders(0)
                    .shippingOrders(0)
                    .lowStockCount(0)
                    .build();
        }
    }
    
    /**
     * Tính tổng tiền ship từ danh sách đơn hàng (chỉ đơn DELIVERED)
     */
    private BigDecimal calculateShippingFee(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(order -> order.getShippingPrice() != null ? order.getShippingPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính tổng doanh thu từ danh sách đơn hàng (không bao gồm ship)
     * Chỉ tính các đơn đã thanh toán thành công (DELIVERED)
     */
    private BigDecimal calculateRevenue(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalPrice) // Chỉ lấy totalPrice (không có ship)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính % tăng trưởng
     */
    private Double calculateGrowthPercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        
        BigDecimal growth = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return growth.doubleValue();
    }
    
    /**
     * Tính % tăng trưởng cho số nguyên
     */
    private Double calculateGrowthPercent(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        
        return ((double) (current - previous) / previous) * 100;
    }
    
    @Override
    public List<RecentOrderDTO> getRecentOrders() {
        try {
            // Lấy 5 đơn hàng mới nhất, sắp xếp theo thời gian tạo giảm dần
            PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Order> orders = orderRepository.findAll(pageRequest).getContent();
            
            List<RecentOrderDTO> result = new ArrayList<>();
            for (Order order : orders) {
                RecentOrderDTO dto = RecentOrderDTO.builder()
                        .orderId(order.getId())
                        .orderCode(order.getOrderCode())
                        .customerName(order.getUser() != null ? order.getUser().getProfile().getFullName() : "Khách hàng")
                        .totalAmount(order.getFinalPrice())
                        .status(order.getStatus().name())
                        .createdAt(order.getCreatedAt())
                        .build();
                result.add(dto);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error fetching recent orders", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<TopSellingProductDTO> getTopSellingProducts() {
        try {
            // Query để lấy top 5 sản phẩm bán chạy
            // Cần thêm query vào OrderRepository
            List<Object[]> topProducts = orderRepository.findTopSellingProducts(PageRequest.of(0, 5));
            
            List<TopSellingProductDTO> result = new ArrayList<>();
            for (Object[] row : topProducts) {
                TopSellingProductDTO dto = TopSellingProductDTO.builder()
                        .productId(((Number) row[0]).longValue())
                        .productName((String) row[1])
                        .thumbnail((String) row[2])
                        .price((BigDecimal) row[3])
                        .totalSold(((Number) row[4]).longValue())
                        .totalRevenue((BigDecimal) row[5])
                        .build();
                result.add(dto);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error fetching top selling products", e);
            return new ArrayList<>();
        }
    }
}
