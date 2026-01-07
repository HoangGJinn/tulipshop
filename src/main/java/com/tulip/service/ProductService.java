package com.tulip.service;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductCompositeDTO;
import com.tulip.dto.ProductCreateDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ProductDetailDTO getProductDetail(Long productId);
    ProductCardDTO convertToCardDTO(Product p);
    Page<ProductCardDTO> getFilteredProducts(String categorySlug, String color, String size, Double minPrice, Double maxPrice, String tag, Pageable pageable);
    void addVariant(Long productId, String colorName, String colorCode);
    void updateVariantStock(Long variantId, Map<String, Integer> stockData);
    void deleteVariant(Long variantId);
    void CreateFullProduct(ProductCompositeDTO dto);
    List<ProductCardDTO> getRelatedProducts(Long currentProductId, Long categoryId);
    List<ProductCardDTO> getViewedProducts(List<Long> productIds);

    List<Product> findProductsWithDeepDiscount();

    ProductCompositeDTO getProductByIdAsDTO(Long id);

    void updateProduct(Long id, ProductCompositeDTO productDTO);
    
    /**
     * Soft delete - Xóa mềm sản phẩm
     * Kiểm tra tồn kho trước khi xóa
     */
    void deleteProduct(Long productId);
}
