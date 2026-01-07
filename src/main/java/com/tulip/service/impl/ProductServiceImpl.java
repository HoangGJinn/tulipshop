package com.tulip.service.impl;

import com.tulip.dto.*;
import com.tulip.entity.product.*;
import com.tulip.exception.BusinessException;
import com.tulip.repository.*;
import com.tulip.repository.specification.ProductSpecification;
import com.tulip.service.CategoryService;
import com.tulip.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final VariantRepository variantRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductAuditRepository productAuditRepository;

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
                .neckline(dto.getNeckline())
                .material(dto.getMaterial())
                .sleeveType(dto.getSleeveType())
                .brand(dto.getBrand())
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
                // Mapping thuộc tính sản phẩm
                .neckline(product.getNeckline())
                .material(product.getMaterial())
                .sleeveType(product.getSleeveType())
                .brand(product.getBrand())
                .tags(product.getTags())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCardDTO> getFilteredProducts(String categorySlug,
                                                     String color,
                                                     String size,
                                                     Double minPrice,
                                                     Double maxPrice,
                                                     String tag,
                                                     Pageable pageable) {
        
        // 1. Xử lý category IDs (hỗ trợ N-cấp)
        List<Long> categoryIds = null;
        if (categorySlug != null && !categorySlug.isEmpty()) {
            Optional<Category> categoryOpt = categoryService.findBySlug(categorySlug);
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                categoryIds = categoryService.getAllChildCategoryIds(category.getId());
            } else {
                // Category không tồn tại, trả về trang rỗng
                return Page.empty(pageable);
            }
        }
        
        // 2. Xây dựng Specification với tất cả điều kiện lọc
        Specification<Product> spec = ProductSpecification.buildFilterSpec(
            categoryIds, tag, color, size, minPrice, maxPrice
        );
        
        // 3. Query từ database với phân trang
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        
        // 4. Chuyển đổi sang DTO
        return productPage.map(this::convertToCardDTO);
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


    /**
     * Filters a list of products to return only those with ACTIVE status
     * @param products the list of products to filter
     * @return a list containing only active products
     */
    private List<Product> filterActiveProducts(List<Product> products) {
        return products.stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<ProductCardDTO> getRelatedProducts(Long currentProductId, Long categoryId){
        List<Product> products = productRepository.findTop5ByCategoryIdAndIdNot(categoryId, currentProductId);
        return filterActiveProducts(products).stream()
                .map(this::convertToCardDTO)
                .collect(Collectors.toList());

    }

    public List<ProductCardDTO> getViewedProducts(List<Long> productIds){
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> products = productRepository.findAllById(productIds);

        // Sắp xếp lại theo thứ tự mới nhất lên đầu
        Map<Long, Product> productMap = filterActiveProducts(products).stream()
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
        List<Product> products = productRepository.findProductsWithDeepDiscount();
        // Chỉ trả về sản phẩm ACTIVE
        return filterActiveProducts(products);
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
        dto.setStatus(product.getStatus());
        
        // Map thuộc tính kỹ thuật
        dto.setNeckline(product.getNeckline());
        dto.setMaterial(product.getMaterial());
        dto.setSleeveType(product.getSleeveType());
        dto.setBrand(product.getBrand());

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

        // Lưu giá trị cũ để audit
        String oldName = product.getName();
        BigDecimal oldPrice = product.getBasePrice();
        boolean nameChanged = !oldName.equals(dto.getName());
        boolean priceChanged = oldPrice.compareTo(dto.getPrice()) != 0;

        //  Cập nhật thông tin cơ bản
        product.setName(dto.getName());
        product.setBasePrice(dto.getPrice());
        product.setDiscountPrice(dto.getDiscountPrice());
        product.setDescription(dto.getDescription());
        product.setTags(dto.getTags());
        
        // Cập nhật thuộc tính kỹ thuật
        product.setNeckline(dto.getNeckline());
        product.setMaterial(dto.getMaterial());
        product.setSleeveType(dto.getSleeveType());
        product.setBrand(dto.getBrand());
        
        // Cập nhật trạng thái
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

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

        // Tạo audit log chỉ khi tên hoặc giá thay đổi
        if (nameChanged || priceChanged) {
            createAuditLog(product.getId(), oldName, product.getName(), 
                          oldPrice, product.getBasePrice(), nameChanged, priceChanged);
        }
    }

    /**
     * Soft Delete - Xóa mềm sản phẩm
     * Kiểm tra tồn kho trước khi xóa
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));

        // Kiểm tra tổng tồn kho của tất cả variants
        int totalStock = productStockRepository.sumQuantityByProductId(productId);
        
        if (totalStock > 0) {
            throw new BusinessException(
                "Không thể xóa sản phẩm vẫn còn tồn kho. " +
                "Vui lòng điều chỉnh kho về 0 trước. Tồn kho hiện tại: " + totalStock
            );
        }

        // Thực hiện soft delete
        product.setStatus(ProductStatus.DELETED);
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    /**
     * Tạo bản ghi audit log khi tên hoặc giá thay đổi
     */
    private void createAuditLog(Long productId, String oldName, String newName,
                               BigDecimal oldPrice, BigDecimal newPrice,
                               boolean nameChanged, boolean priceChanged) {
        String changeType;
        if (nameChanged && priceChanged) {
            changeType = "BOTH";
        } else if (nameChanged) {
            changeType = "NAME_CHANGE";
        } else {
            changeType = "PRICE_CHANGE";
        }

        String changedBy = getCurrentUserEmail();

        ProductAudit audit = ProductAudit.builder()
                .productId(productId)
                .oldName(nameChanged ? oldName : null)
                .newName(nameChanged ? newName : null)
                .oldPrice(priceChanged ? oldPrice : null)
                .newPrice(priceChanged ? newPrice : null)
                .changedBy(changedBy)
                .changeType(changeType)
                .build();

        productAuditRepository.save(audit);
    }

    /**
     * Lấy email của user hiện tại từ SecurityContext
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "system";
    }


}