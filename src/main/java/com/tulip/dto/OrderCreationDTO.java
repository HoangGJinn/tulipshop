package com.tulip.dto;

import lombok.Data;

@Data
public class OrderCreationDTO {
    private Long addressId;         // ID địa chỉ người dùng chọn
    private String paymentMethod;   // "COD", "VNPAY", "MOMO"
    private String note;            // Ghi chú đơn hàng (nếu có)
}