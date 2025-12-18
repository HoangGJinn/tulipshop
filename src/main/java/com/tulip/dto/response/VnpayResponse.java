package com.tulip.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VnpayResponse {
    private String code;        // Mã trạng thái (VD: 200, 400)
    private String message;     // Thông báo chi tiết
    private String paymentUrl;  // Link dẫn sang VNPAY
}