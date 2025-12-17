package com.tulip.controller.api;

import com.tulip.dto.ProductSearchDTO;
import com.tulip.entity.product.Product;
import com.tulip.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/store")
@RequiredArgsConstructor
public class StoreApiController {
    private final ProductRepository productRepository;

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchDTO>> searchProducts(@RequestParam("q") String query){
        if (query == null || query.trim().length() < 2){
            return ResponseEntity.ok(List.of()); // rỗng nếu từ khoá quá ngắn
        }

        List<Product> products = productRepository.searchSmart(query.trim());

        List<ProductSearchDTO> results = products.stream()
                .limit(6)
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);

    }

    private ProductSearchDTO convertToSearchDTO(Product p) {
        return ProductSearchDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .thumbnail(p.getThumbnail())
                .price(p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(java.math.BigDecimal.ZERO) > 0 ? p.getDiscountPrice() : p.getBasePrice())
                .originalPrice(p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(java.math.BigDecimal.ZERO) > 0 ? p.getBasePrice() : null)
                .url("/product/" + p.getId())
                .build();
    }

    // Hàng mới về
    @GetMapping("/new-arrivals")
    public ResponseEntity<List<ProductSearchDTO>> getNewArrivals() {
        return ResponseEntity.ok(productRepository.findTop5ByOrderByIdDesc().stream()
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList()));
    }

    // Sale > 18%
    @GetMapping("/sale-18")
    public ResponseEntity<List<ProductSearchDTO>> getSale18() {
        return ResponseEntity.ok(productRepository.findProductsDiscountOver18().stream()
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList()));
    }

    // Đang thịnh hành (Random)
    @GetMapping("/trending")
    public ResponseEntity<List<ProductSearchDTO>> getTrending() {
        return ResponseEntity.ok(productRepository.findRandomProducts().stream()
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList()));
    }

    // Lấy sản phẩm theo Occasion (Dịp: di-lam, di-choi, di-tiec)
    @GetMapping("/occasion")
    public ResponseEntity<List<ProductSearchDTO>> getProductsByOccasion(@RequestParam("tag") String tag) {
        // Gọi repository tìm theo tag
        List<Product> products = productRepository.findByTagsContainingIgnoreCase(tag);

        // Convert sang DTO và giới hạn 10 sản phẩm
        List<ProductSearchDTO> results = products.stream()
                .limit(10)
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

}
