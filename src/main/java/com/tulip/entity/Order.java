package com.tulip.entity;

import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.entity.enums.PaymentStatus;
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
    private OrderStatus status; // PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED, RETURNED

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod; // COD, VNPAY, MOMO

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus; // PENDING, SUCCESS, FAILED, EXPIRED

    @Column(name = "transaction_id", length = 255)
    private String transactionId; // Mã giao dịch từ nhà cung cấp thanh toán

    @Column(name = "vnp_txn_ref", length = 255)
    private String vnpTxnRef;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "payment_expire_at")
    private LocalDateTime paymentExpireAt;

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
}