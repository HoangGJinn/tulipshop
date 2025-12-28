package com.tulip.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockInitRequest {
    private Long variantId;
    private Integer sizeId;
    private Integer initialQuantity;
}
