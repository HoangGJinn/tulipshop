package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    private Integer quantity; // Số lượng tồn kho

    @Column(unique = true)
    private String sku; // Mã quản lý kho: VAY-TRANG-S
}