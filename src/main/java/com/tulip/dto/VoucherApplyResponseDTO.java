package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class VoucherApplyResponseDTO {
    private boolean success;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;
    private VoucherDTO voucher; // Thông tin voucher nếu áp dụng thành công
}

