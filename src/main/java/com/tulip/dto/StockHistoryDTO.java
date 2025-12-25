package com.tulip.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockHistoryDTO {
    private Long id;
    private Long stockId;
    private LocalDateTime timestamp;
    private Integer previousQuantity;
    private Integer newQuantity;
    private Integer changeAmount;
    private String adminUsername;
    private String reason;
}
