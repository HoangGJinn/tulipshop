package com.tulip.controller;

import com.tulip.dto.ProductCardDTO;
import com.tulip.dto.ProductDetailDTO;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.ProductService;
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

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/product/{id}")
    public String viewProductDetail(@PathVariable Long id, Model model) {
        ProductDetailDTO productDTO = productService.getProductDetail(id);

        // Truyền dữ liệu xuống View
        model.addAttribute("product", productDTO);

        // Dữ liệu này sẽ được JS dùng để xử lý logic chọn màu/size
        model.addAttribute("productJson", productDTO);

        return "product-detail";
    }


    @GetMapping("/products")
    public String viewAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model) {

        // Lấy danh sách sản phẩm đã lọc
        List<ProductCardDTO> products = productService.getFilteredProducts(category, sort, color, size, minPrice, maxPrice);

        // Lấy dữ liệu bổ trợ cho Filter
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("products", products);

        // Giữ lại các tham số filter để hiển thị trên giao diện
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", sort);

        return "products";
    }

}