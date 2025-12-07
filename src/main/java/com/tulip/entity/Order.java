package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // "order" là từ khóa SQL nên cần đặt tên bảng là "orders"
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "total_price")
    private BigDecimal totalPrice; // Tổng tiền hàng

    @Column(name = "shipping_price")
    private BigDecimal shippingPrice;

    @Column(name = "final_price")
    private BigDecimal finalPrice; // Tổng tiền phải trả (sau khi trừ voucher + ship)

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, SHIPPING, COMPLETED, CANCELLED

    @Column(name = "payment_method")
    private String paymentMethod; // COD, VNPOINTS, BANKING

    @Column(name = "shipping_address")
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED, RETURNED
    }
}