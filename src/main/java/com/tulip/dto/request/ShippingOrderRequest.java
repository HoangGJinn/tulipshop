package com.tulip.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingOrderRequest {
    private String orderCode;       // Mã đơn hàng (VD: ORD-12345)
    private String receiverName;    // Tên người nhận
    private String receiverPhone;   // SĐT người nhận
    private String receiverAddress; // Địa chỉ chi tiết
    private String deliveryType;    // STANDARD hoặc FAST
    private BigDecimal codAmount;   // Số tiền thu hộ (0 nếu đã thanh toán online)
}