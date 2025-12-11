package com.tulip.controller.admin;

import com.tulip.dto.ProductCompositeDTO;
import com.tulip.dto.ProductCreateDTO;
import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductVariant;
import com.tulip.entity.product.ProductVariantImage;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.repository.VariantRepository;
import com.tulip.service.ProductService;
import com.tulip.service.impl.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("productDTO", new ProductCompositeDTO());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("allSizes", sizeRepository.findAll());
        return "admin/product-create";
    }

    // 1. Sửa lại hàm createProduct để chuyển hướng sang trang Edit thay vì trang chi tiết
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createProduct(@ModelAttribute ProductCompositeDTO productDTO) {
        try{
            productService.CreateFullProduct(productDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm sản phẩm thành công");
            response.put("redirectUrl", "/products");

            return ResponseEntity.ok(response);

        }catch (Exception e){
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Lỗi hệ thống: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }



}