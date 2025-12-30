package com.tulip.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VoucherApplyRequestDTO {
    private String code;
    private BigDecimal orderTotal;
}

