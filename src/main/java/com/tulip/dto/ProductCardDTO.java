package com.tulip.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductCardDTO {
    private Long id;
    private String name;
    private String thumbnail; // Ảnh đại diện chính
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private String categorySlug;

    // List các mã màu để hiển thị chấm tròn
    private List<String> colorCodes;

    // List các ảnh đại diện cho từng màu
    private List<String> colorImages;
}