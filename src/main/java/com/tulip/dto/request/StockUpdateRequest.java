package com.tulip.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockUpdateRequest {
    private Long stockId;
    private Integer newPhysicalStock;
    private String reason;
}
