package com.tulip.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String sku; // Mã sản phẩm chung
    private String description;
    private BigDecimal basePrice;
    private String categoryName;

    // Danh sách tất cả các size có thể có (S, M, L, XL)
    private List<String> allSizes;

    // Danh sách các biến thể màu sắc
    private List<ProductVariantDTO> variants;
}