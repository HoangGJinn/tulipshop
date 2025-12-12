package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductVariantDTO {
    private Long id;
    private String colorName;
    private String colorCode;
    private BigDecimal price;
    private List<String> images;

    private Map<String, StockInfo> stockBySize;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockInfo {
        private Long id;      // Đây chính là stockId cần thiết!
        private int quantity; // Số lượng tồn kho
    }
}