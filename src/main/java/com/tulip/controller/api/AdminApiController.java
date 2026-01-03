package com.tulip.controller.api;

import com.tulip.dto.ProductCompositeDTO;
import com.tulip.dto.AdminNotificationRequest;
import com.tulip.dto.NotificationRequest;
import com.tulip.entity.User;
import com.tulip.entity.Notification;
import com.tulip.dto.NotificationDTO;
import com.tulip.entity.product.Category;
import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductStatus;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.UserRepository;
import com.tulip.repository.NotificationRepository;
import com.tulip.service.ProductService;
import com.tulip.service.NotificationService;
import com.tulip.service.impl.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final com.tulip.service.GoogleAIService googleAIService;
    private final com.tulip.service.CategoryService categoryService;

    // --- API: LẤY DANH SÁCH SẢN PHẨM CHO ADMIN ---
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getProducts() {
        // Admin thấy tất cả sản phẩm ACTIVE và HIDDEN (không thấy DELETED)
        List<com.tulip.entity.product.ProductStatus> visibleStatuses = List.of(
            com.tulip.entity.product.ProductStatus.ACTIVE,
            com.tulip.entity.product.ProductStatus.HIDDEN
        );
        List<Product> products = productRepository.findByStatusIn(visibleStatuses);
        
        List<Map<String, Object>> productList = products.stream().map(p -> {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", p.getId());
            productMap.put("name", p.getName());
            productMap.put("sku", "SKU-" + p.getId());
            productMap.put("image", p.getThumbnail() != null ? p.getThumbnail() : "/images/placeholder.jpg");
            productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : "Chưa phân loại");
            productMap.put("price", p.getBasePrice().doubleValue());
            productMap.put("status", p.getStatus() != null ? p.getStatus().name() : "ACTIVE");
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
    
    // --- API: CẬP NHẬT TRẠNG THÁI SẢN PHẨM ---
    @PutMapping("/products/{id}/status")
    public ResponseEntity<?> updateProductStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            
            String statusStr = payload.get("status");
            com.tulip.entity.product.ProductStatus newStatus = 
                com.tulip.entity.product.ProductStatus.valueOf(statusStr);
            
            // Nếu chuyển sang DELETED, kiểm tra tồn kho
            if (newStatus == com.tulip.entity.product.ProductStatus.DELETED) {
                productService.deleteProduct(id);
            } else {
                product.setStatus(newStatus);
                productRepository.save(product);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Cập nhật trạng thái thành công"
            ));
        } catch (com.tulip.exception.BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error", 
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error", 
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    // --- API: XÓA SẢN PHẨM (SOFT DELETE) ---
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Xóa sản phẩm thành công!"
            ));
        } catch (com.tulip.exception.BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error", 
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error", 
                "message", "Lỗi: " + e.getMessage()
            ));
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
    
    // --- API: AI GENERATE PRODUCT DESCRIPTION ---
    @PostMapping("/ai/generate-description")
    public ResponseEntity<?> generateProductDescription(@RequestBody com.tulip.dto.AIDescriptionRequest request) {
        try {
            log.info("🤖 AI Request - Product: {}, Image URL: {}", request.getProductName(), request.getThumbnailUrl());
            
            // Tối ưu URL Cloudinary trước khi gửi cho AI (giảm xuống 512px để tiết kiệm tokens)
            String optimizedImageUrl = cloudinaryService.optimizeImageForAI(request.getThumbnailUrl());
            log.info("🔧 Optimized image URL for AI: {}", optimizedImageUrl);
            
            // Gọi AI với URL đã tối ưu (KHÔNG dùng Base64)
            String htmlContent = googleAIService.generateProductDescription(
                request.getProductName(),
                optimizedImageUrl,  // Truyền URL thay vì MultipartFile
                request.getNeckline(),
                request.getMaterial(),
                request.getSleeveType(),
                request.getBrand()
            );
            
            log.info("✅ AI generated description successfully");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "htmlContent", htmlContent
            ));
        } catch (Exception e) {
            log.error("❌ AI Generation Error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Lỗi khi tạo nội dung: " + e.getMessage()
            ));
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



    
    // ==================== NOTIFICATION MANAGEMENT ====================
    
    /**
     * API: Gửi thông báo từ Admin
     * POST /v1/api/admin/notifications/send
     */
    @PostMapping("/notifications/send")
    public ResponseEntity<?> sendNotification(
        @RequestParam("type") String type,
        @RequestParam("targetType") String targetType,
        @RequestParam(value = "recipientEmail", required = false) String recipientEmail,
        @RequestParam("title") String title,
        @RequestParam("content") String content,
        @RequestParam(value = "targetUrl", required = false) String targetUrl,
        @RequestParam(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        try {
            // Validate
            if ("SPECIFIC".equals(targetType) && 
                (recipientEmail == null || recipientEmail.trim().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Vui lòng nhập email người nhận"
                ));
            }
            
            // Upload ảnh nếu có
            String imageUrl = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    imageUrl = cloudinaryService.uploadImage(imageFile);
                    log.info("Đã upload ảnh thông báo: {}", imageUrl);
                } catch (Exception e) {
                    log.error("Lỗi upload ảnh: {}", e.getMessage());
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Lỗi upload ảnh: " + e.getMessage()
                    ));
                }
            }
            
            // Parse notification type
            Notification.NotificationType notificationType;
            try {
                notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Loại thông báo không hợp lệ"
                ));
            }
            
            // Tạo notification request
            NotificationRequest notificationRequest = NotificationRequest.builder()
                .title(title)
                .content(content)
                .link(targetUrl)
                .imageUrl(imageUrl)
                .type(notificationType)
                .build();
            
            if ("ALL".equals(targetType)) {
                // Gửi broadcast (chỉ lưu 1 bản ghi với user = NULL)
                NotificationDTO result = notificationService.sendBroadcastNotification(notificationRequest);
                
                log.info("✅ Đã gửi thông báo broadcast thành công: {}", result.getTitle());
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đã gửi thông báo đến tất cả người dùng",
                    "notification", result
                ));
                
            } else {
                // Gửi cho người dùng cụ thể
                User user = userRepository.findByEmail(recipientEmail)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + recipientEmail));
                
                NotificationDTO result = notificationService.sendNotification(user.getEmail(), notificationRequest);
                
                log.info("✅ Đã gửi thông báo đến user {} thành công: {}", recipientEmail, result.getTitle());
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đã gửi thông báo đến " + recipientEmail,
                    "notification", result
                ));
            }
            
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Lấy danh sách thông báo đã gửi (cho Admin)
     * GET /v1/api/admin/notifications
     * Chỉ hiển thị thông báo PROMOTION và SYSTEM
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // Chỉ lấy thông báo PROMOTION và SYSTEM
            Page<Notification> notifications = notificationRepository.findByTypeIn(
                List.of(Notification.NotificationType.PROMOTION, Notification.NotificationType.SYSTEM),
                pageable
            );
            
            List<Map<String, Object>> data = notifications.getContent().stream()
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("title", n.getTitle());
                    map.put("content", n.getContent());
                    map.put("imageUrl", n.getImageUrl());
                    map.put("link", n.getLink());
                    map.put("type", n.getType().name());
                    map.put("targetType", n.getUser() == null ? "ALL" : "SPECIFIC");
                    map.put("recipientEmail", n.getUser() != null ? n.getUser().getEmail() : null);
                    map.put("createdAt", n.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", data,
                "total", notifications.getTotalElements(),
                "page", page,
                "size", size
            ));
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * API: Xóa thông báo
     * DELETE /v1/api/admin/notifications/{id}
     */
    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
            
            // Chỉ cho phép xóa thông báo PROMOTION và SYSTEM
            if (notification.getType() != Notification.NotificationType.PROMOTION &&
                notification.getType() != Notification.NotificationType.SYSTEM) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Chỉ được phép xóa thông báo Khuyến mãi và Hệ thống"
                ));
            }
            
            notificationRepository.delete(notification);
            
            log.info("✅ Đã xóa thông báo ID: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đã xóa thông báo thành công"
            ));
        } catch (Exception e) {
            log.error("Lỗi xóa thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // ==================== CATEGORY MANAGEMENT ====================
    
    /**
     * API: Lấy danh sách tất cả categories
     * GET /v1/api/admin/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Category> categories = categoryService.findAll();
            
            List<Map<String, Object>> data = categories.stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    map.put("slug", c.getSlug());
                    map.put("parentId", c.getParent() != null ? c.getParent().getId() : null);
                    map.put("parentName", c.getParent() != null ? c.getParent().getName() : null);
                    map.put("hasProducts", categoryService.hasProducts(c.getId()));
                    map.put("childrenCount", c.getChildren() != null ? c.getChildren().size() : 0);
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", data
            ));
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách categories: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * API: Thêm mới category
     * POST /v1/api/admin/categories
     */
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            Object parentIdObj = payload.get("parentId");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Tên danh mục không được để trống"
                ));
            }
            
            Category category = new Category();
            category.setName(name.trim());
            
            // Xử lý parent category
            if (parentIdObj != null) {
                Long parentId = null;
                if (parentIdObj instanceof Integer) {
                    parentId = ((Integer) parentIdObj).longValue();
                } else if (parentIdObj instanceof Long) {
                    parentId = (Long) parentIdObj;
                } else if (parentIdObj instanceof String) {
                    try {
                        parentId = Long.parseLong((String) parentIdObj);
                    } catch (NumberFormatException e) {
                        // Ignore invalid parent ID
                    }
                }
                
                if (parentId != null && parentId > 0) {
                    Category parent = categoryService.findById(parentId)
                        .orElseThrow(() -> new com.tulip.exception.BusinessException("Không tìm thấy danh mục cha"));
                    category.setParent(parent);
                }
            }
            
            Category saved = categoryService.save(category);
            
            log.info("✅ Đã tạo category mới: {} (ID: {})", saved.getName(), saved.getId());
            
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", saved.getId());
            categoryData.put("name", saved.getName());
            categoryData.put("slug", saved.getSlug());
            categoryData.put("parentId", saved.getParent() != null ? saved.getParent().getId() : null);
            categoryData.put("parentName", saved.getParent() != null ? saved.getParent().getName() : null);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Thêm danh mục thành công",
                "data", categoryData
            ));
        } catch (com.tulip.exception.BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Lỗi tạo category: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Cập nhật category
     * PUT /v1/api/admin/categories/{id}
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            Category category = categoryService.findById(id)
                .orElseThrow(() -> new com.tulip.exception.BusinessException("Không tìm thấy danh mục"));
            
            String name = (String) payload.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Tên danh mục không được để trống"
                ));
            }
            
            category.setName(name.trim());
            
            // Xử lý parent category
            Object parentIdObj = payload.get("parentId");
            if (parentIdObj != null) {
                Long parentId = null;
                if (parentIdObj instanceof Integer) {
                    parentId = ((Integer) parentIdObj).longValue();
                } else if (parentIdObj instanceof Long) {
                    parentId = (Long) parentIdObj;
                } else if (parentIdObj instanceof String) {
                    try {
                        parentId = Long.parseLong((String) parentIdObj);
                    } catch (NumberFormatException e) {
                        // Ignore invalid parent ID
                    }
                }
                
                if (parentId != null && parentId > 0) {
                    // Kiểm tra không cho phép set parent là chính nó
                    if (parentId.equals(id)) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Không thể đặt danh mục làm cha của chính nó"
                        ));
                    }
                    
                    // Kiểm tra không cho phép set parent là con cháu của nó
                    List<Long> childIds = categoryService.getAllChildCategoryIds(id);
                    if (childIds.contains(parentId)) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Không thể đặt danh mục con làm cha"
                        ));
                    }
                    
                    Category parent = categoryService.findById(parentId)
                        .orElseThrow(() -> new com.tulip.exception.BusinessException("Không tìm thấy danh mục cha"));
                    category.setParent(parent);
                } else {
                    category.setParent(null);
                }
            } else {
                category.setParent(null);
            }
            
            Category updated = categoryService.save(category);
            
            log.info("✅ Đã cập nhật category: {} (ID: {})", updated.getName(), updated.getId());
            
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", updated.getId());
            categoryData.put("name", updated.getName());
            categoryData.put("slug", updated.getSlug());
            categoryData.put("parentId", updated.getParent() != null ? updated.getParent().getId() : null);
            categoryData.put("parentName", updated.getParent() != null ? updated.getParent().getName() : null);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cập nhật danh mục thành công",
                "data", categoryData
            ));
        } catch (com.tulip.exception.BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Lỗi cập nhật category: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Lấy danh sách sản phẩm theo category
     * GET /v1/api/admin/categories/{id}/products
     */
    @GetMapping("/categories/{id}/products")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long id) {
        try {
            // Kiểm tra category tồn tại
            Category category = categoryService.findById(id)
                .orElseThrow(() -> new com.tulip.exception.BusinessException("Không tìm thấy danh mục"));
            
            // Lấy tất cả category IDs (bao gồm cả con cháu)
            List<Long> categoryIds = categoryService.getAllChildCategoryIds(id);
            
            // Lấy sản phẩm ACTIVE và HIDDEN (không lấy DELETED)
            List<ProductStatus> activeStatuses = List.of(ProductStatus.ACTIVE, ProductStatus.HIDDEN);
            List<Product> products = productRepository.findByStatusIn(activeStatuses).stream()
                .filter(p -> p.getCategory() != null && categoryIds.contains(p.getCategory().getId()))
                .collect(Collectors.toList());
            
            // Map sang DTO đơn giản
            List<Map<String, Object>> productList = products.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("status", p.getStatus().name());
                    map.put("thumbnail", p.getThumbnail());
                    map.put("categoryName", p.getCategory().getName());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "categoryName", category.getName(),
                "productCount", productList.size(),
                "products", productList
            ));
        } catch (com.tulip.exception.BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Lỗi lấy sản phẩm theo category: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Xóa category
     * DELETE /v1/api/admin/categories/{id}
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteById(id);
            
            log.info("✅ Đã xóa category ID: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Xóa danh mục thành công"
            ));
        } catch (com.tulip.exception.BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Lỗi xóa category: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
}
