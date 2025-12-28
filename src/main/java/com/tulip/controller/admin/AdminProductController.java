package com.tulip.controller.admin;

import com.tulip.dto.ProductCompositeDTO;
import com.tulip.entity.product.Category;
import com.tulip.entity.product.Product;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String showProductList(Model model) {
        List<Product> allProducts = productRepository.findAll();
        model.addAttribute("products", allProducts);

        long totalProducts = allProducts.size();
        
        long lowStockProducts = allProducts.stream()
            .filter(p -> {
                int totalStock = p.getVariants().stream()
                    .flatMap(v -> v.getStocks().stream())
                    .mapToInt(s -> s.getQuantity())
                    .sum();
                return totalStock <= 5;
            })
            .count();

        BigDecimal totalValue = allProducts.stream()
            .map(p -> {
                 int quantity = p.getVariants().stream()
                    .flatMap(v -> v.getStocks().stream())
                    .mapToInt(s -> s.getQuantity())
                    .sum();
                 return p.getBasePrice().multiply(BigDecimal.valueOf(quantity));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Map<String, Object>> stats = new ArrayList<>();
        
        Map<String, Object> stat1 = new HashMap<>();
        stat1.put("label", "Tổng sản phẩm");
        stat1.put("value", totalProducts);
        stat1.put("colorClass", "text-black");
        stat1.put("icon", "fas fa-box");
        stats.add(stat1);
        
        Map<String, Object> stat2 = new HashMap<>();
        stat2.put("label", "Sắp hết hàng");
        stat2.put("value", lowStockProducts);
        stat2.put("colorClass", "text-red-500");
        stat2.put("icon", "fas fa-exclamation-triangle");
        stats.add(stat2);
        
        Map<String, Object> stat3 = new HashMap<>();
        stat3.put("label", "Tổng giá trị kho");
        stat3.put("value", String.format("%,.0f", totalValue.doubleValue()) + "₫");
        stat3.put("colorClass", "text-green-600");
        stat3.put("icon", "fas fa-money-bill-wave");
        stats.add(stat3);
        
        Map<String, Object> stat4 = new HashMap<>();
        stat4.put("label", "Danh mục");
        stat4.put("value", categoryRepository.count());
        stat4.put("colorClass", "text-blue-500");
        stat4.put("icon", "fas fa-tags");
        stats.add(stat4);
        
        model.addAttribute("stats", stats);
        
        model.addAttribute("pageTitle", "Quản lý sản phẩm");
        model.addAttribute("currentPage", "products");
        model.addAttribute("contentTemplate", "admin/products/list");
        model.addAttribute("showSearch", true);
        
        List<String> tableHeaders = List.of("Ảnh", "Sản phẩm", "Danh mục", "Giá bán", "Tổng kho", "Thao tác");
        model.addAttribute("tableHeaders", tableHeaders);
        
        return "admin/layouts/layout";
    }

    @GetMapping("/form")
    public String showProductForm(@RequestParam(value = "id", required = false) Long id, Model model) {
        ProductCompositeDTO productDTO;

        if (id != null) {
            productDTO = productService.getProductByIdAsDTO(id);
        } else {
            productDTO = new ProductCompositeDTO();
            productDTO.setTags("");
        }

        model.addAttribute("productDTO", productDTO);
        model.addAttribute("allSizes", sizeRepository.findAll());
        model.addAttribute("isEditMode", id != null);

        List<Category> allCategories = categoryRepository.findAll();
        List<CategoryOption> hierarchicalCategories = new ArrayList<>();
        for (Category cat : allCategories) {
            if (cat.getParent() == null) {
                buildCategoryHierarchy(cat, hierarchicalCategories, 0);
            }
        }
        model.addAttribute("categories", hierarchicalCategories);

        model.addAttribute("pageTitle", id != null ? "Cập nhật sản phẩm" : "Thêm sản phẩm mới");
        model.addAttribute("currentPage", "products");
        model.addAttribute("contentTemplate", "admin/products/form");

        return "admin/layouts/layout";
    }

    private void buildCategoryHierarchy(Category current, List<CategoryOption> list, int level) {
        String prefix = "";
        for (int i = 0; i < level; i++) prefix += "— ";
        
        list.add(new CategoryOption(current.getId(), current.getSlug(), prefix + current.getName()));
        
        for (Category child : current.getChildren()) {
            buildCategoryHierarchy(child, list, level + 1);
        }
    }

    public record CategoryOption(Long id, String slug, String displayName) {}
}