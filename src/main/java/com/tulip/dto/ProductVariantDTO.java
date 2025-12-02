package com.tulip.dto;

import lombok.Builder;
import lombok.Data;
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
    private List<String> images; // List URL ảnh của màu này

    // Map lưu số lượng tồn kho theo size. Ví dụ: "S" -> 10, "M" -> 0
    private Map<String, Integer> stockBySize;
}