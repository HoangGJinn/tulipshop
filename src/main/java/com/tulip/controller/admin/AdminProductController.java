package com.tulip.controller.admin;

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
        model.addAttribute("productDTO", new ProductCreateDTO());
        model.addAttribute("categories", categoryRepository.findAll()); // Để đổ vào dropdown danh mục
        model.addAttribute("allSizes", sizeRepository.findAll());
        return "admin/product-create"; // Trả về file HTML
    }

    // 1. Sửa lại hàm createProduct để chuyển hướng sang trang Edit thay vì trang chi tiết
    @PostMapping("/create")
    public String createProduct(@ModelAttribute ProductCreateDTO productDTO) {
        Long newProductId = productService.createProduct(productDTO);
        return "redirect:/admin/products/edit/" + newProductId; // <--- Đổi dòng này
    }

    // 2. Hiển thị trang Chỉnh sửa (Bước 2)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        model.addAttribute("product", product);
        return "admin/product-edit";
    }

    // 3. API Upload ảnh cho từng biến thể (Variant)
    @PostMapping("/variant/upload-image")
    public String uploadVariantImage(@RequestParam("variantId") Long variantId,
                                     @RequestParam("imageFile") MultipartFile file) {

        if (!file.isEmpty()) {
            // A. Upload lên Cloud
            String imageUrl = cloudinaryService.uploadImage(file);

            // B. Tìm Variant và Lưu link ảnh
            ProductVariant variant = variantRepository.findById(variantId).orElseThrow();

            ProductVariantImage image = ProductVariantImage.builder()
                    .variant(variant)
                    .imageUrl(imageUrl)
                    .build();

            variant.getImages().add(image);
            variantRepository.save(variant);
        }

        // Reload lại trang edit của sản phẩm đó
        ProductVariant v = variantRepository.findById(variantId).get();
        return "redirect:/admin/products/edit/" + v.getProduct().getId();
    }


    @PostMapping("/variant/update-color")
    public String updateVariantColor(@RequestParam("variantId") Long variantId,
                                     @RequestParam("colorCode") String colorCode) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        variant.setColorCode(colorCode);
        variantRepository.save(variant);

        return "redirect:/admin/products/edit/" + variant.getProduct().getId();
    }

    // 1. Xử lý Thêm Màu Mới
    @PostMapping("/variant/add")
    public String addVariant(@RequestParam Long productId,
                             @RequestParam String colorName,
                             @RequestParam String colorCode) {
        productService.addVariant(productId, colorName, colorCode);
        return "redirect:/admin/products/edit/" + productId;
    }

    // 2. Xử lý Xóa Màu
    @GetMapping("/variant/delete/{id}")
    public String deleteVariant(@PathVariable Long id) {
        ProductVariant variant = variantRepository.findById(id).orElseThrow();
        Long productId = variant.getProduct().getId();
        productService.deleteVariant(id);
        return "redirect:/admin/products/edit/" + productId;
    }

    // 3. Xử lý Cập nhật Kho hàng (Stock)
    @PostMapping("/variant/update-stock")
    public String updateStock(@RequestParam Long variantId,
                              @RequestParam Map<String, String> allParams) {

        // Lọc ra các tham số bắt đầu bằng "stock_" (Ví dụ: stock_S, stock_M)
        Map<String, Integer> stockMap = new HashMap<>();

        allParams.forEach((key, value) -> {
            if (key.startsWith("stock_")) {
                String sizeCode = key.substring(6); // Cắt bỏ chữ "stock_" lấy "S"
                try {
                    stockMap.put(sizeCode, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    stockMap.put(sizeCode, 0);
                }
            }
        });

        productService.updateVariantStock(variantId, stockMap);

        // Redirect về trang edit
        ProductVariant v = variantRepository.findById(variantId).get();
        return "redirect:/admin/products/edit/" + v.getProduct().getId();
    }

}