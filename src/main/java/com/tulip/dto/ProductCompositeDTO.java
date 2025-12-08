package com.tulip.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ProductCompositeDTO {
    private String name;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String description;
    private String categorySlug;
    private MultipartFile mainImageFile;

    // Danh sách biến thể (Màu sắc)
    private List<VariantInput> variants = new ArrayList<>();

    @Data
    public static class VariantInput {
        private String colorName;
        private String colorCode;
        private List<MultipartFile> imageFiles;

        private Map<String, Integer> stockPerSize;
    }
}
