package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIDescriptionRequest {
    private String productName;
    private String thumbnailUrl;
    private String neckline;
    private String material;
    private String sleeveType;
    private String brand;
}
