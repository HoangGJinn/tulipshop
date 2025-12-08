package com.tulip.service.impl;

import com.tulip.dto.*;
import com.tulip.entity.product.*;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.repository.VariantRepository;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryRepository categoryRepository;
    private final VariantRepository variantRepository;

    @Transactional
    public void CreateFullProduct(ProductCompositeDTO dto){
        String mainThumbnail = "https://placehold.co/600x800?text=No+Image";
        if (dto.getMainImageFile() != null && !dto.getMainImageFile().isEmpty()) {
            mainThumbnail = cloudinaryService.uploadImage(dto.getMainImageFile());
        }

        Category category = categoryRepository.findBySlug(dto.getCategorySlug())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        Product product = Product.builder()
                .name(dto.getName())
                .basePrice(dto.getPrice())
                .discountPrice(dto.getDiscountPrice())
                .description(dto.getDescription())
                .category(category)
                .thumbnail(mainThumbnail)
                .variants(new ArrayList<>())
                .build();

        Product savedProduct = productRepository.save(product);

        if (dto.getVariants() != null) {
            for (ProductCompositeDTO.VariantInput vInput : dto.getVariants()) {
                // Bỏ qua nếu không nhập tên màu
                if (vInput.getColorName() == null || vInput.getColorName().isBlank()) continue;
                // Upload ảnh của Variant này (nếu có)
                String variantImageUrl = null;
                if (vInput.getImageFiles() != null && !vInput.getImageFiles().isEmpty()) {
                    variantImageUrl = cloudinaryService.uploadImage(vInput.getImageFiles());
                }

                // Tạo Variant Entity
                ProductVariant variant = ProductVariant.builder()
                        .product(savedProduct)
                        .colorName(vInput.getColorName())
                        .colorCode(vInput.getColorCode())
                        .images(new ArrayList<>())
                        .stocks(new ArrayList<>())
                        .build();

                // Lưu ảnh vào list ảnh của variant
                if (variantImageUrl != null) {
                    variant.getImages().add(ProductVariantImage.builder()
                            .variant(variant)
                            .imageUrl(variantImageUrl)
                            .build());
                }

                // Tạo Stock (Kho hàng) từ Map
                if (vInput.getStockPerSize() != null) {
                    for (Map.Entry<String, Integer> entry : vInput.getStockPerSize().entrySet()) {
                        String sizeCode = entry.getKey();
                        Integer quantity = entry.getValue();

                        sizeRepository.findByCode(sizeCode).ifPresent(size -> {
                            ProductStock stock = ProductStock.builder()
                                    .variant(variant)
                                    .size(size)
                                    .quantity(quantity != null ? quantity : 0)
                                    .sku(savedProduct.getId() + "-" + vInput.getColorName() + "-" + sizeCode)
                                    .build();
                            variant.getStocks().add(stock);
                        });
                    }
                }

                variantRepository.save(variant);
            }
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Lấy danh sách tất cả các Size để hiển thị lên giao diện
        List<String> allSizeCodes = sizeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Size::getSortOrder))
                .map(Size::getCode)
                .collect(Collectors.toList());

        // Chuyển đổi danh sách Variant Entity -> DTO
        List<ProductVariantDTO> variantDTOs = product.getVariants().stream().map(variant -> {
            // Key là Size Code (S, M...), Value là số lượng
            Map<String, Integer> stockMap = new HashMap<>();
            // Key là Size Code (S, M...), Value là stockId
            Map<String, Long> stockIdsMap = new HashMap<>();
            variant.getStocks().forEach(stock -> {
                stockMap.put(stock.getSize().getCode(), stock.getQuantity());
                stockIdsMap.put(stock.getSize().getCode(), stock.getId());
            });

            // Lấy danh sách ảnh
            List<String> images = variant.getImages().stream()
                    .map(ProductVariantImage::getImageUrl)
                    .collect(Collectors.toList());

            return ProductVariantDTO.builder()
                    .id(variant.getId())
                    .colorName(variant.getColorName())
                    .colorCode(variant.getColorCode())
                    .price(product.getBasePrice()) // Có thể lấy variant.getPrice() nếu giá khác nhau
                    .images(images)
                    .stockBySize(stockMap)
                    .stockIdsBySize(stockIdsMap)
                    .build();
        }).collect(Collectors.toList());

        return ProductDetailDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getVariants().isEmpty() || product.getVariants().get(0).getStocks().isEmpty()
                        ? "SKU-" + product.getId()
                        : product.getVariants().get(0).getStocks().get(0).getSku().split("-")[0] + "-" + product.getId())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                .allSizes(allSizeCodes)
                .variants(variantDTOs)
                .build();
    }

    public List<ProductCardDTO> getFilteredProducts(String categorySlug,
                                                    String sort, String color,
                                                    String size, Double minPrice,
                                                    Double maxPrice) {

        List<Product> products = productRepository.findAll();

        return products.stream()
                // Lọc Category
                .filter(p -> categorySlug == null || categorySlug.isEmpty() ||
                        (p.getCategory() != null && p.getCategory().getSlug().equals(categorySlug)))

                // Lọc Giá
                .filter(p -> (minPrice == null || p.getBasePrice().doubleValue() >= minPrice) &&
                        (maxPrice == null || p.getBasePrice().doubleValue() <= maxPrice))

                // Lọc Màu
                .filter(p -> color == null || color.isEmpty() ||
                        p.getVariants().stream().anyMatch(v -> v.getColorName().equalsIgnoreCase(color)))

                // Lọc Size
                .filter(p -> size == null || size.isEmpty() ||
                        p.getVariants().stream().anyMatch(v ->
                                v.getStocks().stream().anyMatch(s -> s.getSize().getCode().equalsIgnoreCase(size) && s.getQuantity() > 0)
                        ))

                // Sắp xếp
                .sorted((p1, p2) -> {
                    if ("price_asc".equals(sort)) return p1.getBasePrice().compareTo(p2.getBasePrice());
                    if ("price_desc".equals(sort)) return p2.getBasePrice().compareTo(p1.getBasePrice());
                    return p2.getId().compareTo(p1.getId()); // Mặc định mới nhất (ID lớn nhất)
                })


                .map(this::convertToCardDTO)
                .collect(Collectors.toList());
    }

    public ProductCardDTO convertToCardDTO(Product p) {
        BigDecimal priceToShow = p.getBasePrice();
        BigDecimal originPrice = null;
        Integer discountPercent = null;

        if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
            && p.getDiscountPrice().compareTo(p.getBasePrice()) < 0){
                priceToShow = p.getDiscountPrice();
                originPrice = p.getBasePrice();
                BigDecimal diff = p.getBasePrice().subtract(p.getDiscountPrice());
                discountPercent = diff.divide(p.getBasePrice(), 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100)).intValue();

        }

        // Lấy list màu
        List<String> colors = p.getVariants().stream().map(ProductVariant::getColorCode).collect(Collectors.toList());
        List<String> colorImgs = p.getVariants().stream()
                .map(v -> v.getImages().isEmpty() ? p.getThumbnail() : v.getImages().get(0).getImageUrl())
                .collect(Collectors.toList());

        return ProductCardDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .thumbnail(p.getThumbnail())
                .price(priceToShow)
                .originalPrice(originPrice)
                .discountPercent(discountPercent)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : "")
                .colorCodes(colors)
                .colorImages(colorImgs)
                .build();
    }

    @Transactional // đảm bảo lưu Product và Variant cùng thành công hoặc cùng thất bại
    public Long createProduct(ProductCreateDTO dto){
        String thumbnailUrl = "https://placehold.co/600x800?text=No+Image";

        if (dto.getThumbnailFile() != null && !dto.getThumbnailFile().isEmpty()) {
            thumbnailUrl = cloudinaryService.uploadImage(dto.getThumbnailFile());
        }

        Category category = categoryRepository.findBySlug(dto.getCategorySlug())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));


        Product product = Product.builder()
                .name(dto.getName())
                .basePrice(dto.getPrice())
                .discountPrice(dto.getDiscountPrice())
                .description(dto.getDescription())
                .category(category)
                .thumbnail(thumbnailUrl)// Lưu link ảnh từ Cloudinary
                .variants(new ArrayList<>())
                .build();

        Product savedProduct = productRepository.save(product);

        if (dto.getColors() != null && !dto.getColors().isBlank()) {
            // Tách chuỗi "Trắng, Đen" thành mảng ["Trắng", "Đen"]
            String[] colorList = dto.getColors().split(",");

            List<Size> selectedSizes = new ArrayList<>();
            if (dto.getSizes() != null && !dto.getSizes().isEmpty()) {
                selectedSizes = sizeRepository.findAll().stream()
                        .filter(s -> dto.getSizes().contains(s.getCode()))
                        .toList();

            }else{
                selectedSizes = sizeRepository.findAll();
            }



            for (String colorName : colorList){
                colorName = colorName.trim(); // xoá khoảng trắng thừa
                if (colorName.isEmpty()) continue;

                ProductVariant variant = ProductVariant.builder()
                        .product(savedProduct)
                        .colorName(colorName)
                        .colorCode("#000000") // để tạm
                        .stocks(new ArrayList<>())
                        .build();

                // Tạo Stock (Kho hàng) cho từng Size (Mặc định số lượng = 0)
                for (Size size : selectedSizes) {
                    ProductStock stock = ProductStock.builder()
                            .variant(variant)
                            .size(size)
                            .quantity(0) // Mặc định 0
                            .sku(savedProduct.getId() + "-" + colorName.toUpperCase() + "-" + size.getCode())
                            .build();
                    variant.getStocks().add(stock);
                }

                variantRepository.save(variant);

            }
        }
        return savedProduct.getId();
    }

    @Transactional
    public void addVariant(Long productId, String colorName, String colorCode) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .colorName(colorName)
                .colorCode(colorCode)
                .build();

        // Tự động tạo kho hàng = 0 cho tất cả các size hiện có
        List<Size> sizes = sizeRepository.findAll();
        List<ProductStock> stocks = new ArrayList<>();

        for (Size size : sizes) {
            ProductStock stock = ProductStock.builder()
                    .variant(variant)
                    .size(size)
                    .quantity(0) // Mặc định 0
                    .sku(product.getId() + "-" + colorName.toUpperCase() + "-" + size.getCode())
                    .build();
            stocks.add(stock);
        }
        variant.setStocks(stocks);
        variantRepository.save(variant);
    }

    // Hàm cập nhật kho hàng (Nhận vào Map: key="S", value=10)
    @Transactional
    public void updateVariantStock(Long variantId, Map<String, Integer> stockData) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant không tồn tại"));

        for (ProductStock stock : variant.getStocks()) {
            String sizeCode = stock.getSize().getCode();
            if (stockData.containsKey(sizeCode)) {
                stock.setQuantity(stockData.get(sizeCode));
            }
        }
        variantRepository.save(variant);
    }

    // Hàm xóa variant
    @Transactional
    public void deleteVariant(Long variantId) {
        variantRepository.deleteById(variantId);
    }

}