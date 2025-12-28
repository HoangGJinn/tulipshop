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
import org.springframework.web.multipart.MultipartFile;

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
                .tags(dto.getTags())
                .build();

        Product savedProduct = productRepository.save(product);

        if (dto.getVariants() != null) {
            for (ProductCompositeDTO.VariantInput vInput : dto.getVariants()) {
                if (vInput.getColorName() == null || vInput.getColorName().isBlank()) continue;

                ProductVariant variant = ProductVariant.builder()
                        .product(savedProduct)
                        .colorName(vInput.getColorName())
                        .colorCode(vInput.getColorCode())
                        .images(new ArrayList<>())
                        .stocks(new ArrayList<>())
                        .build();


                if (vInput.getImageFiles() != null) {
                    for (MultipartFile file : vInput.getImageFiles()) {
                        if (!file.isEmpty()) {
                            String url = cloudinaryService.uploadImage(file);

                            ProductVariantImage image = ProductVariantImage.builder()
                                    .variant(variant)
                                    .imageUrl(url)
                                    .build();
                            variant.getImages().add(image);
                        }
                    }
                }

                if (vInput.getStockPerSize() != null) {
                    for (Map.Entry<String, Integer> entry : vInput.getStockPerSize().entrySet()) {
                        String sizeCode = entry.getKey();
                        Integer quantity = entry.getValue();

                        // Only create stock record if quantity > 0
                        if (quantity != null && quantity > 0) {
                            sizeRepository.findByCode(sizeCode).ifPresent(size -> {
                                ProductStock stock = ProductStock.builder()
                                        .variant(variant)
                                        .size(size)
                                        .quantity(quantity)
                                        .sku(savedProduct.getId() + "-" + vInput.getColorName() + "-" + sizeCode)
                                        .build();
                                variant.getStocks().add(stock);
                            });
                        }
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

        List<String> allSizeCodes = sizeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Size::getSortOrder))
                .map(Size::getCode)
                .collect(Collectors.toList());

        List<ProductVariantDTO> variantDTOs = product.getVariants().stream().map(variant -> {

            Map<String, ProductVariantDTO.StockInfo> stockMap = new HashMap<>();

            variant.getStocks().forEach(stock ->
                    stockMap.put(
                            stock.getSize().getCode(),
                            new ProductVariantDTO.StockInfo(stock.getId(), stock.getQuantity())
                    )
            );

            List<String> images = variant.getImages().stream()
                    .map(ProductVariantImage::getImageUrl)
                    .collect(Collectors.toList());

            return ProductVariantDTO.builder()
                    .id(variant.getId())
                    .colorName(variant.getColorName())
                    .colorCode(variant.getColorCode())
                    .price(product.getBasePrice())
                    .images(images)
                    .stockBySize(stockMap)
                    .build();
        }).collect(Collectors.toList());


        return ProductDetailDTO.builder()
                .id(product.getId())
                .name(product.getName())
                // Logic tạo SKU hiển thị
                .sku(product.getVariants().isEmpty() || product.getVariants().get(0).getStocks().isEmpty()
                        ? "SKU-" + product.getId()
                        : product.getVariants().get(0).getStocks().get(0).getSku().split("-")[0] + "-" + product.getId())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : 0)
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



    @Transactional
    public void addVariant(Long productId, String colorName, String colorCode) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .colorName(colorName)
                .colorCode(colorCode)
                .stocks(new ArrayList<>()) // Initialize empty list, no default stocks
                .build();

        // Don't create any stock records by default
        // Admin will add stock through inventory management
        variantRepository.save(variant);
    }


    @Transactional
    public void updateVariantStock(Long variantId, Map<String, Integer> stockData) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant không tồn tại"));

        for (Map.Entry<String, Integer> entry : stockData.entrySet()) {
            String sizeCode = entry.getKey();
            Integer newQuantity = entry.getValue();

            // Find existing stock for this size
            Optional<ProductStock> existingStockOpt = variant.getStocks().stream()
                    .filter(s -> s.getSize().getCode().equals(sizeCode))
                    .findFirst();

            if (existingStockOpt.isPresent()) {
                ProductStock existingStock = existingStockOpt.get();
                
                if (newQuantity != null && newQuantity > 0) {
                    // Update existing stock
                    existingStock.setQuantity(newQuantity);
                } else {
                    // Remove stock record if quantity becomes 0 or null
                    variant.getStocks().remove(existingStock);
                }
            } else {
                // Create new stock record only if quantity > 0
                if (newQuantity != null && newQuantity > 0) {
                    sizeRepository.findByCode(sizeCode).ifPresent(size -> {
                        ProductStock newStock = ProductStock.builder()
                                .variant(variant)
                                .size(size)
                                .quantity(newQuantity)
                                .sku(variant.getProduct().getId() + "-" + variant.getColorName() + "-" + sizeCode)
                                .build();
                        variant.getStocks().add(newStock);
                    });
                }
            }
        }
        variantRepository.save(variant);
    }


    @Transactional
    public void deleteVariant(Long variantId) {
        variantRepository.deleteById(variantId);
    }


    public List<ProductCardDTO> getRelatedProducts(Long currentProductId, Long categoryId){
        List<Product> products = productRepository.findTop5ByCategoryIdAndIdNot(categoryId, currentProductId);
        return products.stream()
                .map(this::convertToCardDTO)
                .collect(Collectors.toList());

    }

    public List<ProductCardDTO> getViewedProducts(List<Long> productIds){
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> products = productRepository.findAllById(productIds);

        // Sắp xếp lại theo thứ tự mới nhất lên đầu
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        List<ProductCardDTO> result = new ArrayList<>();
        for (Long id : productIds){
            if (productMap.containsKey(id)) {
                result.add(convertToCardDTO(productMap.get(id)));
            }
        }
        return result;

    }

    @Override
    public List<Product> findProductsWithDeepDiscount() {
        return productRepository.findProductsWithDeepDiscount();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCompositeDTO getProductByIdAsDTO(Long id) {
        // 1. Tìm sản phẩm
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));

        // 2. Map thông tin chung
        ProductCompositeDTO dto = new ProductCompositeDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getBasePrice());
        dto.setDiscountPrice(product.getDiscountPrice());
        dto.setDescription(product.getDescription());
        dto.setTags(product.getTags());

        // Gán URL ảnh cũ để hiển thị
        dto.setThumbnailUrl(product.getThumbnail());

        if (product.getCategory() != null) {
            dto.setCategorySlug(product.getCategory().getSlug());
        }

        // 3. Map biến thể (Variants)
        List<ProductCompositeDTO.VariantInput> variantInputs = new ArrayList<>();

        for (ProductVariant variant : product.getVariants()) {
            ProductCompositeDTO.VariantInput vInput = new ProductCompositeDTO.VariantInput();

            // Map ID để phân biệt update
            vInput.setId(variant.getId());
            vInput.setColorName(variant.getColorName());
            vInput.setColorCode(variant.getColorCode());

            // Lấy danh sách URL ảnh cũ của biến thể
            List<String> urls = variant.getImages().stream()
                    .map(ProductVariantImage::getImageUrl)
                    .collect(Collectors.toList());
            vInput.setImageUrls(urls);

            // Map tồn kho (Size -> Quantity)
            Map<String, Integer> stockMap = new HashMap<>();
            for (ProductStock stock : variant.getStocks()) {
                if (stock.getSize() != null) {
                    stockMap.put(stock.getSize().getCode(), stock.getQuantity());
                }
            }
            vInput.setStockPerSize(stockMap);

            variantInputs.add(vInput);
        }

        dto.setVariants(variantInputs);
        return dto;
    }

    @Override
    @Transactional
    public void updateProduct(Long id, ProductCompositeDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));

        //  Cập nhật thông tin cơ bản
        product.setName(dto.getName());
        product.setBasePrice(dto.getPrice());
        product.setDiscountPrice(dto.getDiscountPrice());
        product.setDescription(dto.getDescription());
        product.setTags(dto.getTags());

        // Cập nhật Category
        if (dto.getCategorySlug() != null) {
            Category category = categoryRepository.findBySlug(dto.getCategorySlug())
                    .orElseThrow(() -> new RuntimeException("Category không tồn tại"));
            product.setCategory(category);
        }

        // Cập nhật Ảnh đại diện (Chỉ upload nếu user chọn file mới)
        if (dto.getMainImageFile() != null && !dto.getMainImageFile().isEmpty()) {
            String newUrl = cloudinaryService.uploadImage(dto.getMainImageFile());
            product.setThumbnail(newUrl);
        }

        //  Xử lý Variants (Thêm mới hoặc Cập nhật)
        if (dto.getVariants() != null) {
            for (ProductCompositeDTO.VariantInput vInput : dto.getVariants()) {
                // Bỏ qua dòng rỗng
                if (vInput.getColorName() == null || vInput.getColorName().isBlank()) continue;

                if (vInput.getId() != null) {
                    // --- UPDATE VARIANT CŨ ---
                    variantRepository.findById(vInput.getId()).ifPresent(existingVariant -> {
                        existingVariant.setColorName(vInput.getColorName());
                        existingVariant.setColorCode(vInput.getColorCode());

                        // Upload thêm ảnh cho variant cũ (nếu có)
                        if (vInput.getImageFiles() != null) {
                            if (existingVariant.getImages() == null) {
                                existingVariant.setImages(new ArrayList<>());
                            }
                            for (MultipartFile file : vInput.getImageFiles()) {
                                if (!file.isEmpty()) {
                                    String url = cloudinaryService.uploadImage(file);
                                    ProductVariantImage img = ProductVariantImage.builder()
                                            .variant(existingVariant)
                                            .imageUrl(url).build();
                                    existingVariant.getImages().add(img);
                                }
                            }
                        }

                        // Cập nhật Stock
                        if (vInput.getStockPerSize() != null) {
                            for (Map.Entry<String, Integer> entry : vInput.getStockPerSize().entrySet()) {
                                String sizeCode = entry.getKey();
                                Integer qty = entry.getValue();

                                // Tìm stock của size này
                                Optional<ProductStock> stockOpt = existingVariant.getStocks().stream()
                                        .filter(s -> s.getSize() != null && s.getSize().getCode().equals(sizeCode))
                                        .findFirst();

                                if (stockOpt.isPresent()) {
                                    // Existing stock record
                                    if (qty != null && qty > 0) {
                                        // Update quantity
                                        stockOpt.get().setQuantity(qty);
                                    } else {
                                        // Remove stock record if quantity becomes 0 or null
                                        existingVariant.getStocks().remove(stockOpt.get());
                                    }
                                } else {
                                    // Size mới chưa có trong variant này -> Tạo mới chỉ khi qty > 0
                                    if (qty != null && qty > 0) {
                                        sizeRepository.findByCode(sizeCode).ifPresent(size -> {
                                            ProductStock newStock = ProductStock.builder()
                                                    .variant(existingVariant)
                                                    .size(size)
                                                    .quantity(qty)
                                                    .sku(product.getId() + "-" + vInput.getColorName() + "-" + sizeCode)
                                                    .build();
                                            existingVariant.getStocks().add(newStock);
                                        });
                                    }
                                }
                            }
                        }
                        variantRepository.save(existingVariant);
                    });

                } else {
                    // --- TẠO VARIANT MỚI (Logic giống Create) ---
                    ProductVariant newVariant = ProductVariant.builder()
                            .product(product)
                            .colorName(vInput.getColorName())
                            .colorCode(vInput.getColorCode())
                            .images(new ArrayList<>())
                            .stocks(new ArrayList<>())
                            .build();

                    // Upload ảnh
                    if (vInput.getImageFiles() != null) {
                        for (MultipartFile file : vInput.getImageFiles()) {
                            if (!file.isEmpty()) {
                                String url = cloudinaryService.uploadImage(file);
                                ProductVariantImage img = ProductVariantImage.builder()
                                        .variant(newVariant)
                                        .imageUrl(url).build();
                                newVariant.getImages().add(img);
                            }
                        }
                    }

                    // Tạo Stock
                    if (vInput.getStockPerSize() != null) {
                        for (Map.Entry<String, Integer> entry : vInput.getStockPerSize().entrySet()) {
                            String sizeCode = entry.getKey();
                            Integer qty = entry.getValue();
                            
                            // Only create stock record if quantity > 0
                            if (qty != null && qty > 0) {
                                sizeRepository.findByCode(sizeCode).ifPresent(size -> {
                                    ProductStock stock = ProductStock.builder()
                                            .variant(newVariant)
                                            .size(size)
                                            .quantity(qty)
                                            .sku(product.getId() + "-" + vInput.getColorName() + "-" + sizeCode)
                                            .build();
                                    newVariant.getStocks().add(stock);
                                });
                            }
                        }
                    }
                    product.getVariants().add(newVariant);
                    variantRepository.save(newVariant);
                }
            }
        }
        productRepository.save(product);
    }


}