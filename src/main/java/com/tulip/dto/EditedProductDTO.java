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
public class EditedProductDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private String categoryName;
    private BigDecimal currentPrice;
    private Integer totalStock;
    private String status;
    
    // Thông tin chỉnh sửa cuối cùng
    private String lastChangeType;
    private String oldName;
    private String newName;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String changedBy;
    private LocalDateTime changedAt;
    
    // Số lần chỉnh sửa
    private Integer editCount;
}

