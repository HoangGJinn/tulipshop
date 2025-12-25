package com.tulip.controller.api;

import com.tulip.dto.ProductCompositeDTO;
import com.tulip.entity.product.Category;
import com.tulip.entity.product.Product;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.service.ProductService;
import com.tulip.service.impl.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductService productService;

    // --- API: LẤY DANH SÁCH SẢN PHẨM CHO ADMIN ---
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getProducts() {
        List<Product> products = productRepository.findAll();
        List<Map<String, Object>> productList = products.stream().map(p -> {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", p.getId());
            productMap.put("name", p.getName());
            productMap.put("sku", "SKU-" + p.getId());
            productMap.put("image", p.getThumbnail() != null ? p.getThumbnail() : "/images/placeholder.jpg");
            productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : "Chưa phân loại");
            productMap.put("price", p.getBasePrice().doubleValue());
            // Tính tổng stock
            int totalStock = p.getVariants().stream()
                .flatMap(v -> v.getStocks().stream())
                .mapToInt(s -> s.getQuantity())
                .sum();
            productMap.put("stock", totalStock);
            return productMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(productList);
    }
    
    // --- API: XÓA SẢN PHẨM ---
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            
            productRepository.delete(product);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Xóa sản phẩm thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Lỗi: " + e.getMessage()));
        }
    }

    // --- API: THÊM DANH MỤC NHANH ---
    @PostMapping("/products/quick-add-category")
    public ResponseEntity<?> quickAddCategory(@RequestBody Map<String, String> payload) {
        try {
            String name = payload.get("name");
            String parentIdStr = payload.get("parentId");

            Category category = new Category();
            category.setName(name);
            category.setSlug(name.toLowerCase().replace(" ", "-").replaceAll("[^a-z0-9-]", ""));

            if (parentIdStr != null && !parentIdStr.isEmpty() && !parentIdStr.equals("-1")) {
                Category parent = categoryRepository.findById(Long.parseLong(parentIdStr)).orElse(null);
                category.setParent(parent);
            }

            Category saved = categoryRepository.save(category);
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", saved.getId());
            categoryData.put("slug", saved.getSlug());
            categoryData.put("displayName", saved.getName());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "category", categoryData
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // --- API: UPLOAD ẢNH TỪ TINYMCE LÊN CLOUDINARY ---
    @PostMapping("/products/upload-image")
    public ResponseEntity<?> uploadImageFromEditor(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
            }

            // Upload lên Cloudinary
            String imageUrl = cloudinaryService.uploadImage(file);

            // TinyMCE cần response format: { "location": "url" }
            Map<String, String> response = new HashMap<>();
            response.put("location", imageUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi upload ảnh: " + e.getMessage()));
        }
    }

    // --- API: LƯU SẢN PHẨM (THÊM MỚI HOẶC CẬP NHẬT) ---
    @PostMapping("/products/save")
    public ResponseEntity<?> saveOrUpdateProduct(@ModelAttribute ProductCompositeDTO productDTO) {
        try {
            if (productDTO.getId() != null) {
                // --- UPDATE ---
                productService.updateProduct(productDTO.getId(), productDTO);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Cập nhật sản phẩm thành công!"
                ));
            } else {
                // --- CREATE ---
                // Validate ảnh
                if (productDTO.getMainImageFile() == null || productDTO.getMainImageFile().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ảnh đại diện!"));
                }
                productService.CreateFullProduct(productDTO);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Thêm sản phẩm mới thành công!"
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }
}

