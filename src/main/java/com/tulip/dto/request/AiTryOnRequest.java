package com.tulip.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTryOnRequest {
    private String person_url;
    private String cloth_url;
    private String category;
}
