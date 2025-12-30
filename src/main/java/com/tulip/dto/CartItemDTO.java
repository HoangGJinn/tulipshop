package com.tulip.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CartItemDTO {
    private Long id; // ID của CartItem
    private Long stockId; // ID của ProductStock
    private Long productId; // ID sản phẩm (để link tới trang detail)
    private String productName;
    private String productImage;
    private String colorName;
    private String sizeCode;
    private BigDecimal price; // Giá đơn vị
    private Integer quantity;
    private BigDecimal subTotal; // Tổng tiền item này (price * quantity)
    private Integer maxStock; // Tồn kho tối đa có thể mua
}