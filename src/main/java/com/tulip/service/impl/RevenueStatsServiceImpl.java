package com.tulip.service.impl;

import com.tulip.entity.Order;
import com.tulip.entity.RevenueStats;
import com.tulip.entity.RevenueStats.StatsType;
import com.tulip.entity.User;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.repository.OrderRepository;
import com.tulip.repository.RevenueStatsRepository;
import com.tulip.repository.UserRepository;
import com.tulip.service.RevenueStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RevenueStatsServiceImpl implements RevenueStatsService {
    
    private final RevenueStatsRepository revenueStatsRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public RevenueStats calculateAndSaveDailyStats(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        // Kiểm tra xem đã có stats chưa
        return revenueStatsRepository.findByStatsDateAndStatsType(date, StatsType.DAILY)
                .map(existing -> updateStats(existing, startOfDay, endOfDay, date))
                .orElseGet(() -> createNewStats(startOfDay, endOfDay, date, StatsType.DAILY));
    }
    
    @Override
    @Transactional
    public RevenueStats calculateAndSaveWeeklyStats(LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(6); // 7 ngày
        LocalDateTime startOfWeek = startDate.atStartOfDay();
        LocalDateTime endOfWeek = endDate.atTime(LocalTime.MAX);
        
        return revenueStatsRepository.findByStatsDateAndStatsType(endDate, StatsType.WEEKLY)
                .map(existing -> updateStats(existing, startOfWeek, endOfWeek, endDate))
                .orElseGet(() -> createNewStats(startOfWeek, endOfWeek, endDate, StatsType.WEEKLY));
    }
    
    @Override
    @Transactional
    public RevenueStats calculateAndSaveMonthlyStats(LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(LocalTime.MAX);
        
        return revenueStatsRepository.findByStatsDateAndStatsType(endOfMonth, StatsType.MONTHLY)
                .map(existing -> updateStats(existing, startDateTime, endDateTime, endOfMonth))
                .orElseGet(() -> createNewStats(startDateTime, endDateTime, endOfMonth, StatsType.MONTHLY));
    }
    
    @Override
    @Transactional
    public RevenueStats calculateAndSaveQuarterlyStats(LocalDate endDate) {
        int quarter = (endDate.getMonthValue() - 1) / 3;
        LocalDate startOfQuarter = LocalDate.of(endDate.getYear(), quarter * 3 + 1, 1);
        LocalDate endOfQuarter = startOfQuarter.plusMonths(2).withDayOfMonth(startOfQuarter.plusMonths(2).lengthOfMonth());
        LocalDateTime startDateTime = startOfQuarter.atStartOfDay();
        LocalDateTime endDateTime = endOfQuarter.atTime(LocalTime.MAX);
        
        return revenueStatsRepository.findByStatsDateAndStatsType(endOfQuarter, StatsType.QUARTERLY)
                .map(existing -> updateStats(existing, startDateTime, endDateTime, endOfQuarter))
                .orElseGet(() -> createNewStats(startDateTime, endDateTime, endOfQuarter, StatsType.QUARTERLY));
    }
    
    @Override
    public RevenueStats getDailyStats(LocalDate date) {
        return revenueStatsRepository.findByStatsDateAndStatsType(date, StatsType.DAILY)
                .orElse(null);
    }
    
    @Override
    public List<RevenueStats> getLast7Days() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6); // 7 ngày: hôm nay và 6 ngày trước
        
        // Lấy data từ database
        List<RevenueStats> existingStats = revenueStatsRepository.findLast7Days(startDate, today);
        
        // Tạo Map để dễ lookup
        Map<LocalDate, RevenueStats> statsMap = existingStats.stream()
                .collect(Collectors.toMap(RevenueStats::getStatsDate, stat -> stat));
        
        // Tạo list đầy đủ 7 ngày, fill data hoặc tạo record rỗng
        List<RevenueStats> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            RevenueStats stat = statsMap.get(date);
            
            if (stat == null) {
                // Ngày không có data, tạo record rỗng
                stat = RevenueStats.builder()
                        .statsDate(date)
                        .statsType(StatsType.DAILY)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0)
                        .customerCount(0)
                        .pendingOrders(0)
                        .shippingOrders(0)
                        .deliveredOrders(0)
                        .cancelledOrders(0)
                        .build();
            }
            result.add(stat);
        }
        
        return result;
    }
    
    @Override
    public List<RevenueStats> getLast30Days() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29); // 30 ngày (bao gồm hôm nay)
        
        // Lấy data từ database
        List<RevenueStats> existingStats = revenueStatsRepository.findLast30Days(startDate, today);
        
        // Tạo Map để dễ lookup
        Map<LocalDate, RevenueStats> statsMap = existingStats.stream()
                .collect(Collectors.toMap(RevenueStats::getStatsDate, stat -> stat));
        
        // Tạo list đầy đủ 30 ngày
        List<RevenueStats> result = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            RevenueStats stat = statsMap.get(date);
            
            if (stat == null) {
                // Ngày không có data, tạo record rỗng
                stat = RevenueStats.builder()
                        .statsDate(date)
                        .statsType(StatsType.DAILY)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0)
                        .customerCount(0)
                        .pendingOrders(0)
                        .shippingOrders(0)
                        .deliveredOrders(0)
                        .cancelledOrders(0)
                        .build();
            }
            result.add(stat);
        }
        
        return result;
    }
    
    @Override
    public List<RevenueStats> getCurrentMonthDays() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        // Lấy data từ database
        List<RevenueStats> existingStats = revenueStatsRepository.findCurrentMonthDays(startOfMonth, endOfMonth);
        
        // Tạo Map để dễ lookup
        Map<LocalDate, RevenueStats> statsMap = existingStats.stream()
                .collect(Collectors.toMap(RevenueStats::getStatsDate, stat -> stat));
        
        // Tạo list đầy đủ các ngày trong tháng
        List<RevenueStats> result = new ArrayList<>();
        LocalDate currentDate = startOfMonth;
        while (!currentDate.isAfter(endOfMonth)) {
            RevenueStats stat = statsMap.get(currentDate);
            
            if (stat == null) {
                // Ngày không có data, tạo record rỗng
                stat = RevenueStats.builder()
                        .statsDate(currentDate)
                        .statsType(StatsType.DAILY)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0)
                        .customerCount(0)
                        .pendingOrders(0)
                        .shippingOrders(0)
                        .deliveredOrders(0)
                        .cancelledOrders(0)
                        .build();
            }
            result.add(stat);
            currentDate = currentDate.plusDays(1);
        }
        
        return result;
    }
    
    @Override
    public List<RevenueStats> getLast3Months() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(2).withDayOfMonth(1); // 3 tháng gần nhất
        
        // Lấy tất cả DAILY records của 3 tháng
        List<RevenueStats> dailyStats = revenueStatsRepository.findLast3Months(startDate);
        
        // Group by tháng và tính tổng
        Map<LocalDate, RevenueStats> monthlyStatsMap = new java.util.HashMap<>();
        
        for (RevenueStats dailyStat : dailyStats) {
            LocalDate monthKey = dailyStat.getStatsDate().withDayOfMonth(1);
            
            monthlyStatsMap.merge(monthKey, dailyStat, (existing, newStat) -> {
                // Cộng dồn các giá trị
                existing.setRevenue(existing.getRevenue().add(newStat.getRevenue()));
                existing.setOrderCount(existing.getOrderCount() + newStat.getOrderCount());
                existing.setCustomerCount(existing.getCustomerCount() + newStat.getCustomerCount());
                existing.setPendingOrders(existing.getPendingOrders() + newStat.getPendingOrders());
                existing.setShippingOrders(existing.getShippingOrders() + newStat.getShippingOrders());
                existing.setDeliveredOrders(existing.getDeliveredOrders() + newStat.getDeliveredOrders());
                existing.setCancelledOrders(existing.getCancelledOrders() + newStat.getCancelledOrders());
                return existing;
            });
        }
        
        // Tạo list đầy đủ 3 tháng
        List<RevenueStats> result = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            LocalDate monthDate = today.minusMonths(i).withDayOfMonth(1);
            RevenueStats stat = monthlyStatsMap.get(monthDate);
            
            if (stat == null) {
                // Tháng không có data, tạo record rỗng
                stat = RevenueStats.builder()
                        .statsDate(monthDate)
                        .statsType(StatsType.MONTHLY)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0)
                        .customerCount(0)
                        .pendingOrders(0)
                        .shippingOrders(0)
                        .deliveredOrders(0)
                        .cancelledOrders(0)
                        .build();
            } else {
                // Đảm bảo statsType là MONTHLY và statsDate là ngày 1
                stat.setStatsDate(monthDate);
                stat.setStatsType(StatsType.MONTHLY);
            }
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * Tạo stats mới
     */
    private RevenueStats createNewStats(LocalDateTime start, LocalDateTime end, LocalDate statsDate, StatsType type) {
        List<Order> orders = orderRepository.findByDateRange(start, end);
        List<User> allUsers = userRepository.findAll();
        
        // Lọc đơn đã giao
        List<Order> deliveredOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .toList();
        
        // Tính revenue (chỉ tổng giá sản phẩm, không bao gồm ship)
        BigDecimal revenue = deliveredOrders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Tính tổng tiền ship riêng
        BigDecimal shippingFee = deliveredOrders.stream()
                .map(order -> order.getShippingPrice() != null ? order.getShippingPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Đếm số đơn theo trạng thái
        int pendingOrders = (int) orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        int shippingOrdersCount = (int) orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPING).count();
        int deliveredOrdersCount = deliveredOrders.size();
        int cancelledOrders = (int) orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        
        // Đếm khách hàng mới
        int customerCount = (int) allUsers.stream()
                .filter(user -> user.getCreatedAt() != null && 
                        user.getCreatedAt().isAfter(start) && 
                        user.getCreatedAt().isBefore(end))
                .count();
        
        RevenueStats stats = RevenueStats.builder()
                .statsDate(statsDate)
                .statsType(type)
                .revenue(revenue) // Doanh thu thuần (không có ship)
                .shippingFee(shippingFee) // Tiền ship riêng
                .orderCount(orders.size())
                .customerCount(customerCount)
                .pendingOrders(pendingOrders)
                .shippingOrders(shippingOrdersCount)
                .deliveredOrders(deliveredOrdersCount)
                .cancelledOrders(cancelledOrders)
                .build();
        
        return revenueStatsRepository.save(stats);
    }
    
    /**
     * Cập nhật stats đã có
     */
    private RevenueStats updateStats(RevenueStats existing, LocalDateTime start, LocalDateTime end, LocalDate statsDate) {
        List<Order> orders = orderRepository.findByDateRange(start, end);
        List<User> allUsers = userRepository.findAll();
        
        // Lọc đơn đã giao
        List<Order> deliveredOrdersList = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .toList();
        
        // Tính revenue (không bao gồm ship)
        BigDecimal revenue = deliveredOrdersList.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Tính tiền ship riêng
        BigDecimal shippingFee = deliveredOrdersList.stream()
                .map(order -> order.getShippingPrice() != null ? order.getShippingPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int pendingOrders = (int) orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        int shippingOrdersCount = (int) orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPING).count();
        int deliveredOrdersCount = deliveredOrdersList.size();
        int cancelledOrders = (int) orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        
        int customerCount = (int) allUsers.stream()
                .filter(user -> user.getCreatedAt() != null && 
                        user.getCreatedAt().isAfter(start) && 
                        user.getCreatedAt().isBefore(end))
                .count();
        
        existing.setRevenue(revenue);
        existing.setShippingFee(shippingFee);
        existing.setOrderCount(orders.size());
        existing.setCustomerCount(customerCount);
        existing.setPendingOrders(pendingOrders);
        existing.setShippingOrders(shippingOrdersCount);
        existing.setDeliveredOrders(deliveredOrdersCount);
        existing.setCancelledOrders(cancelledOrders);
        
        return revenueStatsRepository.save(existing);
    }
}
