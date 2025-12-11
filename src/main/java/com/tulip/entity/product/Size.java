package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String code; // S, M, L, XL

    @Column(name = "sort_order")
    private Integer sortOrder; // 1, 2, 3, 4 để sắp xếp
}