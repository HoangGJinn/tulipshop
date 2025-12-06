package com.tulip.controller;

import com.tulip.dto.ProductCardDTO;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor; // Thêm Lombok để tự autowired
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor // Quan trọng: Để Spring tự tiêm ProductService
public class HomeController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model) {
        // Lấy 8 sản phẩm mới nhất để hiển thị ở trang chủ
        List<ProductCardDTO> products = productService.getFilteredProducts(null, "newest", null, null, null, null);

        if (products.size() > 8) {
            products = products.subList(0, 8);
        }

        model.addAttribute("products", products);
        model.addAttribute("homeCategories", categoryRepository.findAll());
        model.addAttribute("title", "Tulip Shop - Trang chủ");

        return "index";
    }
}