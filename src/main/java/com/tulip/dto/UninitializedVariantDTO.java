package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for product variants that don't have stock records initialized
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UninitializedVariantDTO {
    private Long variantId;
    private Long sizeId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
}
