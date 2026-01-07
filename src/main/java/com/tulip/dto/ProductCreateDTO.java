package com.tulip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateDTO {
    private String name;
    private BigDecimal price;
    private BigDecimal discountPrice; // Giá giảm (nếu có)
    private String description;
    private String categorySlug;      // Để chọn danh mục

    private String colors;

    // Đây là trường quan trọng để hứng file ảnh từ form
    private MultipartFile thumbnailFile;

    private List<String> sizes;
    
    // Thuộc tính kỹ thuật
    private String neckline;    // Kiểu cổ áo
    private String material;    // Chất liệu
    private String sleeveType;  // Kiểu tay áo
    private String brand;       // Nhãn hiệu
}