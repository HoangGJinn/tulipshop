package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType type; // PERCENT hoặc AMOUNT

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "min_order_value")
    private BigDecimal minOrderValue;

    private Integer quantity; // Tổng số lượng voucher phát hành

    @Column(name = "used_count")
    private Integer usedCount; // Số lượng đã dùng

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    private Boolean status; // true = active

    public enum DiscountType {
        PERCENT, AMOUNT
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(status) &&
                (quantity > usedCount) &&
                (startAt == null || now.isAfter(startAt)) &&
                (expireAt == null || now.isBefore(expireAt));
    }
}