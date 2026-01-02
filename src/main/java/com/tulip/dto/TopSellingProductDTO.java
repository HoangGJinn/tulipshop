package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSellingProductDTO {
    
    private Long productId;
    private String productName;
    private String thumbnail;
    private BigDecimal price;
    private Long totalSold;
    private BigDecimal totalRevenue;
}
