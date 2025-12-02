package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    private ProductVariant variant;

    @Column(name = "image_url")
    private String imageUrl;
}