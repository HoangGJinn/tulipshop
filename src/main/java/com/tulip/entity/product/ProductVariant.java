package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude // Tránh vòng lặp vô tận
    private Product product;

    @Column(name = "color_name")
    private String colorName; // VD: Trắng, Đen

    @Column(name = "color_code")
    private String colorCode; // VD: #FFFFFF, #000000

    // Một màu có nhiều ảnh
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    private List<ProductVariantImage> images = new ArrayList<>();

    // Một màu có nhiều kho hàng (theo size)
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    private List<ProductStock> stocks = new ArrayList<>();
}