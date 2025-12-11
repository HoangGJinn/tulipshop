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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreApiController {
    private final ProductRepository productRepository;

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchDTO>> searchProducts(@RequestParam("q") String query){
        if (query == null || query.trim().length() < 2){
            return ResponseEntity.ok(List.of()); // rỗng nếu từ khoá quá ngắn
        }

        List<Product> products = productRepository.findByNameContainingIgnoreCase(query.trim());

        List<ProductSearchDTO> results = products.stream()
                .limit(6)
                .map(p -> ProductSearchDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .thumbnail(p.getThumbnail())
                        .price(p.getDiscountPrice() != null & p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0 ? p.getDiscountPrice() : p.getBasePrice())
                        .originalPrice(p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(java.math.BigDecimal.ZERO) > 0 ? p.getBasePrice() : null)
                        .url("/product/" + p.getId())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);

    }

}
