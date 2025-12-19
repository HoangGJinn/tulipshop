package com.tulip.controller.admin;

import com.tulip.dto.ProductCompositeDTO;
import com.tulip.entity.product.Category;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;

    // --- 1. HIỂN THỊ DANH SÁCH SẢN PHẨM (Trang chủ Admin Products) ---
    // Đây là route mà AJAX sẽ redirect về sau khi lưu thành công
    @GetMapping
    public String showProductList(Model model) {
        // TODO: Thêm logic lấy danh sách sản phẩm từ Service vào đây
        // model.addAttribute("products", productService.getAllProducts());
        return "products"; // File HTML danh sách sản phẩm (bạn cần có file này)
    }

    // --- 2. FORM THÊM/SỬA (Chung 1 Route) ---
    @GetMapping("/form")
    public String showProductForm(@RequestParam(value = "id", required = false) Long id, Model model) {
        ProductCompositeDTO productDTO;

        if (id != null) {
            // Chế độ Sửa
            productDTO = productService.getProductByIdAsDTO(id);
        } else {
            // Chế độ Thêm
            productDTO = new ProductCompositeDTO();
            productDTO.setTags("");
        }

        model.addAttribute("productDTO", productDTO);
        model.addAttribute("allSizes", sizeRepository.findAll());
        model.addAttribute("isEditMode", id != null);

        // Load danh mục phân cấp
        List<Category> allCategories = categoryRepository.findAll();
        List<CategoryOption> hierarchicalCategories = new ArrayList<>();
        for (Category cat : allCategories) {
            if (cat.getParent() == null) {
                buildCategoryHierarchy(cat, hierarchicalCategories, 0);
            }
        }
        model.addAttribute("categories", hierarchicalCategories);

        return "admin/product-form";
    }

    // --- 3. XỬ LÝ LƯU (AJAX API) ---
    // Phải dùng @ResponseBody và trả về ResponseEntity để JS nhận được JSON
    @PostMapping("/save")
    @ResponseBody
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

    // --- CÁC HÀM HELPER KHÁC ---

    // Đệ quy danh mục
    private void buildCategoryHierarchy(Category current, List<CategoryOption> list, int level) {
        String prefix = "";
        for (int i = 0; i < level; i++) prefix += "— ";
        list.add(new CategoryOption(current.getId(), current.getSlug(), prefix + current.getName()));
        for (Category child : current.getChildren()) {
            buildCategoryHierarchy(child, list, level + 1);
        }
    }

    public record CategoryOption(Long id, String slug, String displayName) {}

    // API thêm danh mục nhanh
    @PostMapping("/api/quick-add-category")
    @ResponseBody
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
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "category", new CategoryOption(saved.getId(), saved.getSlug(), saved.getName())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}