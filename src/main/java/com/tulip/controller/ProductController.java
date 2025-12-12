package com.tulip.controller;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingSummaryDTO;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.ProductService;
import com.tulip.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final RatingService ratingService;

    // --- CHI TIẾT SẢN PHẨM ---
    @GetMapping("/product/{id}")
    public String viewProductDetail(@PathVariable Long id, Model model) {
        try {
            ProductDetailDTO productDTO = productService.getProductDetail(id);
            model.addAttribute("product", productDTO);
            // Dữ liệu này sẽ được JS dùng để xử lý logic chọn màu/size
            model.addAttribute("productJson", productDTO);

            List<RatingDTO> reviews = ratingService.getRatingsByProduct(id);
            RatingSummaryDTO ratingSummary = ratingService.getRatingSummary(id);

            model.addAttribute("reviews", reviews);
            model.addAttribute("ratingSummary", ratingSummary);

            return "product-detail";
        } catch (RuntimeException e) {
            return "redirect:/products";
        }
    }

    // --- DANH SÁCH SẢN PHẨM (FILTER) ---
    @GetMapping("/products")
    public String viewAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String priceRange, // Nhận chuỗi "0-500000"
            Model model) {

        // 1. Xử lý logic khoảng giá (Giống ShopController cũ)
        Double minPrice = null;
        Double maxPrice = null;

        if (priceRange != null && !priceRange.isEmpty() && !priceRange.equals("all")) {
            String[] parts = priceRange.split("-");
            if (parts.length >= 1) {
                try { minPrice = Double.parseDouble(parts[0]); } catch (NumberFormatException e) {}
            }
            if (parts.length >= 2 && !parts[1].equals("max")) {
                try { maxPrice = Double.parseDouble(parts[1]); } catch (NumberFormatException e) {}
            }
        }

        // 2. Gọi Service để lọc
        List<ProductCardDTO> products = productService.getFilteredProducts(category, sort, color, size, minPrice, maxPrice);

        // 3. Truyền dữ liệu ra View
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("products", products);

        // Giữ lại các tham số filter để hiển thị trên giao diện (Checked/Selected)
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("selectedColor", color);
        model.addAttribute("selectedSize", size);
        model.addAttribute("selectedPriceRange", priceRange);

        return "products";
    }
}