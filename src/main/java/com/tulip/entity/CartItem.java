package com.tulip.entity;

import com.tulip.entity.product.ProductStock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    @ToString.Exclude
    private Cart cart;

    // Quan trọng: Link tới ProductStock để biết chính xác Size/Màu
    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private ProductStock stock;

    private Integer quantity;

    @CreationTimestamp
    @Column(name = "added_at")
    private LocalDateTime addedAt;

    // Helper method tính tổng tiền tạm tính của item này
    public java.math.BigDecimal getSubTotal() {
        if (stock == null || stock.getVariant() == null || stock.getVariant().getProduct() == null) {
            return java.math.BigDecimal.ZERO;
        }
        // Lấy giá discount nếu có, không thì lấy base price
        var product = stock.getVariant().getProduct();
        var price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getBasePrice();
        return price.multiply(new java.math.BigDecimal(quantity));
    }
}