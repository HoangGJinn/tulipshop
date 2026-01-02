package com.tulip.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingRequest {
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Integer stars;

    @Size(max = 2000, message = "Nội dung đánh giá không được vượt quá 2000 ký tự")
    private String content;

    private String variantInfo;

    private List<MultipartFile> images;
}
