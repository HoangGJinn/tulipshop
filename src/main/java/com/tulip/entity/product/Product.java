package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "status != 'DELETED'")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnail; // Ảnh đại diện chung

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "discount_price")
    private BigDecimal discountPrice;

    @Column(name = "tags")
    private String tags;

    // Thuộc tính kỹ thuật cho bộ lọc
    @Column(name = "neckline")
    private String neckline; // Kiểu cổ áo
    
    @Column(name = "material")
    private String material; // Chất liệu
    
    @Column(name = "sleeve_type")
    private String sleeveType; // Kiểu tay áo
    
    @Column(name = "brand")
    private String brand; // Nhãn hiệu

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}