package com.tulip.controller;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingSummaryDTO;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.ProductService;
import com.tulip.service.RatingService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final RatingService ratingService;

    // --- CHI TIẾT SẢN PHẨM ---
    @GetMapping("/product/{id}")
    public String viewProductDetail(@PathVariable Long id, Model model,
                                    @CookieValue(value = "viewed_products", defaultValue = "") String viewedCookie,
                                    HttpServletResponse response) {


        try {

            // Lấy sản phẩm hiện tại
            ProductDetailDTO productDTO = productService.getProductDetail(id);
            model.addAttribute("product", productDTO);

            Long categoryId = productDTO.getCategoryId();
            List<ProductCardDTO> relatedProducts = productService.getRelatedProducts(id, categoryId);
            model.addAttribute("relatedProducts", relatedProducts);

            List<Long> viewedIds = new ArrayList<>();
            if (!viewedCookie.isEmpty()){
                String[] ids = viewedCookie.split("-");
                for (String strId : ids){
                    try{
                        Long parsedId = Long.parseLong(strId);
                        // Chỉ thêm nếu khác ID hiện tại (để ta đưa ID hiện tại lên đầu sau)
                        if (!parsedId.equals(id)) {
                            viewedIds.add(parsedId);
                        }

                    }catch (NumberFormatException ignored) {

                    }
                }
            }
            // Thêm ID hiện tại vào đầu danh sách
            viewedIds.add(0, id);

            // Giới hạn chỉ lưu 6 sản phẩm gần nhất
            if (viewedIds.size() > 6) {
                viewedIds = viewedIds.subList(0, 6);
            }


            List<Long> idsToShow = new ArrayList<>(viewedIds);
            idsToShow.remove(id); // Xóa cái đang xem khỏi list hiển thị bên dưới
            List<ProductCardDTO> viewedProducts = productService.getViewedProducts(idsToShow);
            model.addAttribute("viewedProducts", viewedProducts);


            // Dữ liệu này sẽ được JS dùng để xử lý logic chọn màu/size
            model.addAttribute("productJson", productDTO);
            String newCookieValue = String.join("-", viewedIds.stream().map(String::valueOf).toArray(String[]::new));

            Cookie cookie = new Cookie("viewed_products", newCookieValue);
            cookie.setMaxAge(60 * 60 * 24 * 30); // Sống 30 ngày
            cookie.setPath("/"); // Có hiệu lực trên toàn bộ website
            response.addCookie(cookie);


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