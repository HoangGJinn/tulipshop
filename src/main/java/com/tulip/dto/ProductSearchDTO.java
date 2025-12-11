package com.tulip.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSearchDTO {
    private Long id;
    private String name;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String url;

}
