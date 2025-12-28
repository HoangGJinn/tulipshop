package com.tulip.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRateRequest {
    private String address;       // Địa chỉ người nhận
    private String deliveryType;  // Loại vận chuyển (STANDARD / FAST)
}