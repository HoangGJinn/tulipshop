package com.tulip.dto;

import com.tulip.entity.product.ProductStatus;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ProductCompositeDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String description;

    private String categorySlug;

    private MultipartFile mainImageFile;

    private String thumbnailUrl; // để hiện ảnh khi sửa

    // Thuộc tính kỹ thuật
    private String neckline;    // Kiểu cổ áo
    private String material;    // Chất liệu
    private String sleeveType;  // Kiểu tay áo
    private String brand;       // Nhãn hiệu

    // Danh sách biến thể (Màu sắc)
    private List<VariantInput> variants = new ArrayList<>();

    // Trạng thái sản phẩm
    private ProductStatus status = ProductStatus.ACTIVE;



    @Data
    public static class VariantInput {
        private Long id;

        private String colorName;
        private String colorCode;
        private List<MultipartFile> imageFiles;

        private Map<String, Integer> stockPerSize;

        private List<String> imageUrls;
    }

    // hứng dữ liệu từ input hidden
    private String tags;
}
