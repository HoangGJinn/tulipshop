package com.tulip.service;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductCreateDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.entity.product.Product;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ProductDetailDTO getProductDetail(Long productId);
    ProductCardDTO convertToCardDTO(Product p);
    List<ProductCardDTO> getFilteredProducts(String categorySlug, String sort, String color, String size, Double minPrice, Double maxPrice);
    Long createProduct(ProductCreateDTO dto);
    void addVariant(Long productId, String colorName, String colorCode);
    void updateVariantStock(Long variantId, Map<String, Integer> stockData);
    void deleteVariant(Long variantId);
}
