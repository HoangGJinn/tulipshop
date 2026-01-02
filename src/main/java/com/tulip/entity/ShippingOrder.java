package com.tulip.entity;

import com.tulip.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;

    @Column(name = "delivery_type")
    private String deliveryType; // FAST, STANDARD

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee; // Phí ship tính toán được

    @Column(name = "cod_amount")
    private BigDecimal codAmount; // Số tiền thu hộ (COD)

    @Column(name = "distance_km")
    private Double distance;

    @Column(name = "estimated_delivery_time")
    private String estimatedDeliveryTime; // Ví dụ: "1-2 ngày"

    // --- TRẠNG THÁI GIAO HÀNG CHI TIẾT ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status; // PREPARING -> PICKING -> DELIVERING -> DELIVERED -> FAILED

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "carrier")
    private String carrier;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}