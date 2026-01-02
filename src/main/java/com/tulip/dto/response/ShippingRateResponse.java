package com.tulip.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRateResponse {
    private String deliveryType;   // STANDARD hoặc FAST
    private BigDecimal shippingFee; // Phí ship
    private double distance;        // Khoảng cách (km)
    private String estimatedTime;   // Thời gian dự kiến (VD: 3-5 ngày)
}