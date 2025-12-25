package com.tulip.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryAlertsDTO {
    private Integer uninitializedCount;
    private Integer lowStockCount;
}
