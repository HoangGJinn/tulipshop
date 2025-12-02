package com.tulip.service.impl;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.dto.ProductVariantDTO;
import com.tulip.entity.product.*;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Lấy danh sách tất cả các Size để hiển thị lên giao diện
        List<String> allSizeCodes = sizeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Size::getSortOrder))
                .map(Size::getCode)
                .collect(Collectors.toList());

        // Chuyển đổi danh sách Variant Entity -> DTO
        List<ProductVariantDTO> variantDTOs = product.getVariants().stream().map(variant -> {
            // Key là Size Code (S, M...), Value là số lượng
            Map<String, Integer> stockMap = new HashMap<>();
            variant.getStocks().forEach(stock ->
                    stockMap.put(stock.getSize().getCode(), stock.getQuantity())
            );

            // Lấy danh sách ảnh
            List<String> images = variant.getImages().stream()
                    .map(ProductVariantImage::getImageUrl)
                    .collect(Collectors.toList());

            return ProductVariantDTO.builder()
                    .id(variant.getId())
                    .colorName(variant.getColorName())
                    .colorCode(variant.getColorCode())
                    .price(product.getBasePrice()) // Có thể lấy variant.getPrice() nếu giá khác nhau
                    .images(images)
                    .stockBySize(stockMap)
                    .build();
        }).collect(Collectors.toList());

        return ProductDetailDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getVariants().isEmpty() || product.getVariants().get(0).getStocks().isEmpty()
                        ? "SKU-" + product.getId()
                        : product.getVariants().get(0).getStocks().get(0).getSku().split("-")[0] + "-" + product.getId())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                .allSizes(allSizeCodes)
                .variants(variantDTOs)
                .build();
    }

    public List<ProductCardDTO> getFilteredProducts(String categorySlug,
                                                    String sort, String color,
                                                    String size, Double minPrice,
                                                    Double maxPrice) {

        List<Product> products = productRepository.findAll();

        return products.stream()
                // Lọc Category
                .filter(p -> categorySlug == null || categorySlug.isEmpty() ||
                        (p.getCategory() != null && p.getCategory().getSlug().equals(categorySlug)))

                // Lọc Giá
                .filter(p -> (minPrice == null || p.getBasePrice().doubleValue() >= minPrice) &&
                        (maxPrice == null || p.getBasePrice().doubleValue() <= maxPrice))

                // Lọc Màu
                .filter(p -> color == null || color.isEmpty() ||
                        p.getVariants().stream().anyMatch(v -> v.getColorName().equalsIgnoreCase(color)))

                // Lọc Size
                .filter(p -> size == null || size.isEmpty() ||
                        p.getVariants().stream().anyMatch(v ->
                                v.getStocks().stream().anyMatch(s -> s.getSize().getCode().equalsIgnoreCase(size) && s.getQuantity() > 0)
                        ))

                // Sắp xếp
                .sorted((p1, p2) -> {
                    if ("price_asc".equals(sort)) return p1.getBasePrice().compareTo(p2.getBasePrice());
                    if ("price_desc".equals(sort)) return p2.getBasePrice().compareTo(p1.getBasePrice());
                    return p2.getId().compareTo(p1.getId()); // Mặc định mới nhất (ID lớn nhất)
                })


                .map(this::convertToCardDTO)
                .collect(Collectors.toList());
    }

    public ProductCardDTO convertToCardDTO(Product p) {
        BigDecimal priceToShow = p.getBasePrice();
        BigDecimal originPrice = null;
        Integer discountPercent = null;

        if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
            && p.getDiscountPrice().compareTo(p.getBasePrice()) < 0){
                priceToShow = p.getDiscountPrice();
                originPrice = p.getBasePrice();
                BigDecimal diff = p.getBasePrice().subtract(p.getDiscountPrice());
                discountPercent = diff.divide(p.getBasePrice(), 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100)).intValue();

        }

        // Lấy list màu
        List<String> colors = p.getVariants().stream().map(ProductVariant::getColorCode).collect(Collectors.toList());
        List<String> colorImgs = p.getVariants().stream()
                .map(v -> v.getImages().isEmpty() ? p.getThumbnail() : v.getImages().get(0).getImageUrl())
                .collect(Collectors.toList());

        return ProductCardDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .thumbnail(p.getThumbnail())
                .price(priceToShow)
                .originalPrice(originPrice)
                .discountPercent(discountPercent)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : "")
                .colorCodes(colors)
                .colorImages(colorImgs)
                .build();
    }

}