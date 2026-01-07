package com.tulip.controller;

import com.tulip.dto.ProductCardDTO;
import com.tulip.entity.product.Product;
import com.tulip.repository.CategoryRepository;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor // Quan trọng: Để Spring tự tiêm ProductService
public class HomeController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model) {
        // Lấy 8 sản phẩm mới nhất để hiển thị ở trang chủ
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductCardDTO> productPage = productService.getFilteredProducts(
            null, null, null, null, null, null, pageable
        );
        List<ProductCardDTO> products = productPage.getContent();

        // Lấy danh sách sản phẩm Sale >= 36%
        List<Product> saleProducts = productService.findProductsWithDeepDiscount();
        saleProducts = saleProducts.stream().limit(10).collect(Collectors.toList());

        model.addAttribute("saleProducts", saleProducts);
        model.addAttribute("products", products);
        model.addAttribute("homeCategories", categoryRepository.findAll());
        model.addAttribute("title", "Tulip Shop - Trang chủ");

        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "Về chúng tôi - Tulip Shop");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Liên hệ - Tulip Shop");
        return "contact";
    }
}