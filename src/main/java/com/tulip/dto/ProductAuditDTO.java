package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAuditDTO {
    private Long id;
    private Long productId;
    private String oldName;
    private String newName;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String changedBy;
    private LocalDateTime changedAt;
    private String changeType;
}
