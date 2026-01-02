package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDTO {
    
    // ID của category
    private Long categoryId;
    
    private String categoryName;

    // Số lượng sản phẩm trong category
    private Long productCount;
    
    // Tổng số lượng tồn kho của category
    private Long totalStock;
    
    // Tỷ lệ phần trăm so với tổng số sản phẩm
    private BigDecimal percentage;
    
    // Màu sắc hiển thị trên biểu đồ
    private String color;
}
