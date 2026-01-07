package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_stats", indexes = {
    @Index(name = "idx_stats_date", columnList = "stats_date"),
    @Index(name = "idx_stats_type_date", columnList = "stats_type,stats_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "stats_date", nullable = false)
    private LocalDate statsDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "stats_type", nullable = false, length = 20)
    private StatsType statsType; // DAILY, WEEKLY, MONTHLY, QUARTERLY
    
    @Column(name = "revenue", precision = 12, scale = 2, nullable = false)
    private BigDecimal revenue; // Doanh thu thuần (không bao gồm ship)
    
    @Column(name = "shipping_fee", precision = 12, scale = 2)
    private BigDecimal shippingFee; // Tổng tiền ship riêng
    
    @Column(name = "order_count", nullable = false)
    private Integer orderCount;
    
    @Column(name = "customer_count", nullable = false)
    private Integer customerCount;
    
    @Column(name = "pending_orders")
    private Integer pendingOrders;
    
    @Column(name = "shipping_orders")
    private Integer shippingOrders;
    
    @Column(name = "delivered_orders")
    private Integer deliveredOrders;
    
    @Column(name = "cancelled_orders")
    private Integer cancelledOrders;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum StatsType {
        DAILY,      // Thống kê theo ngày
        WEEKLY,     // Thống kê theo tuần
        MONTHLY,    // Thống kê theo tháng
        QUARTERLY   // Thống kê theo quý
    }
}
