package com.tulip.service;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.entity.product.Product;

import java.util.List;

public interface ProductService {
    ProductDetailDTO getProductDetail(Long productId);
    ProductCardDTO convertToCardDTO(Product p);
    List<ProductCardDTO> getFilteredProducts(String categorySlug, String sort, String color, String size, Double minPrice, Double maxPrice);
}
