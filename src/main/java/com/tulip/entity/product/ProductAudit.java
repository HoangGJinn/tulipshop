package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_audit", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_changed_at", columnList = "changed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "old_name")
    private String oldName;

    @Column(name = "new_name")
    private String newName;

    @Column(name = "old_price")
    private BigDecimal oldPrice;

    @Column(name = "new_price")
    private BigDecimal newPrice;

    @Column(name = "changed_by", nullable = false)
    private String changedBy; // Email của admin thực hiện thay đổi

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "change_type")
    private String changeType; // NAME_CHANGE, PRICE_CHANGE, BOTH
}
