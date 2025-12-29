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
public class VoucherDTO {
    private Long id;
    private String code;
    private String type; // PERCENT hoặc AMOUNT
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer quantity;
    private Integer usedCount;
    private Integer remaining;
    private LocalDateTime startAt;
    private LocalDateTime expireAt;
    private Boolean status;
    private Boolean isValid;
    private String description;
}

