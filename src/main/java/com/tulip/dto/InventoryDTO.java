package com.tulip.dto;

import com.tulip.entity.enums.StockStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryDTO {
    private Long stockId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private String imageUrl;
    private String colorName;
    private String sizeName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer physicalStock;
    private Integer reservedStock;
    private Integer availableStock;
    private StockStatus status;
    private String categoryName;
    private Long categoryId;
}
