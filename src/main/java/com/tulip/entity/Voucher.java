package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

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

    @Column(length = 255)
    private String name; // Tên voucher (VD: "Voucher Tết 2026", "Quà tặng đánh giá")

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết voucher

    @Enumerated(EnumType.STRING)
    private DiscountType type;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "min_order_value")
    private BigDecimal minOrderValue;

    @Column(name = "start_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @Column(name = "expire_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expireAt;

    private Integer quantity; // Số lượng voucher phát hành

    @Column(name = "used_count")
    private Integer usedCount; // Số lượng đã sử dụng

    private Boolean status;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true; // true = hiển thị công khai, false = chỉ phát riêng cho user

    public enum DiscountType {
        PERCENT, AMOUNT, FREESHIP
    }

    public boolean isValid(BigDecimal orderTotal) {
        LocalDateTime now = LocalDateTime.now();
        int used = usedCount != null ? usedCount : 0;
        int total = quantity != null ? quantity : 0;

        return Boolean.TRUE.equals(status) &&
                (startAt == null || !now.isBefore(startAt)) &&
                (expireAt == null || !now.isAfter(expireAt)) &&
                (minOrderValue == null || orderTotal.compareTo(minOrderValue) >= 0) &&
                (quantity == null || used < total); // Kiểm tra còn voucher
    }
}