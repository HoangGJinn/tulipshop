package com.tulip.controller;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.service.ProductService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

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

            // Ratings được load qua JavaScript API (rating-display.js)
            // Không cần load ở đây để tối ưu performance

            return "product/product-detail";
        } catch (RuntimeException e) {
            return "redirect:/products";
        }
    }

    // --- DANH SÁCH SẢN PHẨM (FILTER) ---
    @GetMapping("/products")
    public String viewAllProducts(
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String priceRange, // Nhận chuỗi "0-500000"
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // 1. Xử lý logic khoảng giá
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

        // 2. Tạo Pageable với sắp xếp
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "id"); // Mặc định: mới nhất
        if ("price_asc".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "basePrice");
        } else if ("price_desc".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "basePrice");
        }
        
        Pageable pageable = PageRequest.of(page, 12, sortOrder);

        // 3. Xử lý collection parameter
        Page<ProductCardDTO> productPage;
        String effectiveCategory = category;
        
        if (collection != null && !collection.isEmpty()) {
            switch (collection) {
                case "ao":
                    effectiveCategory = "ao"; // slug
                    break;
                case "quan":
                    effectiveCategory = "quan"; // slug
                    break;
                case "phu-kien":
                    effectiveCategory = "phu-kien"; // slug
                    break;
                case "gia-tot":
                    // Lấy sản phẩm có discount >= 50%
                    List<ProductCardDTO> giatotProducts = productService.getFilteredProducts(null, sort, color, size, minPrice, maxPrice)
                        .stream()
                        .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() >= 50)
                        .collect(Collectors.toList());
                    
                    // Tạo Page thủ công cho collection đặc biệt
                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), giatotProducts.size());
                    List<ProductCardDTO> pageContent = giatotProducts.subList(start, end);
                    productPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, giatotProducts.size());
                    
                    model.addAttribute("categories", categoryRepository.findAll());
                    model.addAttribute("products", productPage.getContent());
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", productPage.getTotalPages());
                    model.addAttribute("totalElements", productPage.getTotalElements());
                    model.addAttribute("selectedCollection", collection);
                    model.addAttribute("selectedCategory", category);
                    model.addAttribute("selectedSort", sort);
                    model.addAttribute("selectedColor", color);
                    model.addAttribute("selectedSize", size);
                    model.addAttribute("selectedPriceRange", priceRange);
                    return "product/products";
                    
                case "best-seller":
                    // Lấy sản phẩm bán chạy từ repository
                    List<ProductCardDTO> bestsellerProducts = productRepository.findBestSellingProducts()
                        .stream()
                        .map(productService::convertToCardDTO)
                        .collect(Collectors.toList());
                    
                    // Áp dụng các filter khác
                    Double finalMinPrice = minPrice;
                    Double finalMaxPrice = maxPrice;
                    bestsellerProducts = bestsellerProducts.stream()
                        .filter(p -> (finalMinPrice == null || p.getPrice().doubleValue() >= finalMinPrice) &&
                                    (finalMaxPrice == null || p.getPrice().doubleValue() <= finalMaxPrice))
                        .filter(p -> color == null || color.isEmpty() || 
                                    p.getColorCodes().stream().anyMatch(c -> c.equalsIgnoreCase(color)))
                        .filter(p -> size == null || size.isEmpty()) // Size filter cần logic phức tạp hơn
                        .collect(Collectors.toList());
                    
                    // Tạo Page thủ công
                    int startBs = (int) pageable.getOffset();
                    int endBs = Math.min((startBs + pageable.getPageSize()), bestsellerProducts.size());
                    List<ProductCardDTO> pageContentBs = bestsellerProducts.subList(startBs, endBs);
                    productPage = new org.springframework.data.domain.PageImpl<>(pageContentBs, pageable, bestsellerProducts.size());
                    
                    model.addAttribute("categories", categoryRepository.findAll());
                    model.addAttribute("products", productPage.getContent());
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", productPage.getTotalPages());
                    model.addAttribute("totalElements", productPage.getTotalElements());
                    model.addAttribute("selectedCollection", collection);
                    model.addAttribute("selectedCategory", category);
                    model.addAttribute("selectedSort", sort);
                    model.addAttribute("selectedColor", color);
                    model.addAttribute("selectedSize", size);
                    model.addAttribute("selectedPriceRange", priceRange);
                    return "product/products";
                    
                case "all":
                default:
                    // Hiển thị tất cả sản phẩm mới
                    effectiveCategory = null;
                    break;
            }
        }

        // 4. Gọi Service để lọc với phân trang (cho các collection thông thường)
        productPage = productService.getFilteredProductsWithPagination(
            effectiveCategory, sort, color, size, minPrice, maxPrice, pageable
        );

        // 5. Truyền dữ liệu ra View
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());

        // Giữ lại các tham số filter để hiển thị trên giao diện
        model.addAttribute("selectedCollection", collection != null ? collection : "all");
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("selectedColor", color);
        model.addAttribute("selectedSize", size);
        model.addAttribute("selectedPriceRange", priceRange);

        return "product/products";
    }
}