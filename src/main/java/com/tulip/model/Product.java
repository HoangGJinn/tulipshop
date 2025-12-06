package com.tulip.model; // Đã sửa package cho đúng

import java.util.ArrayList;
import java.util.List;

public class Product {
    private Long id;
    private String name;
    private String sku;
    private Double price;
    private Double basePrice;
    private Double originalPrice;
    private Integer discountPercent;
    private String description;
    private String categoryName;
    private String thumbnail;

    private List<String> colorImages;
    private List<String> colorCodes;

    private List<ProductVariant> variants;
    private List<String> allSizes;

    public Product() {}

    public Product(Long id, String name, Double price, Double originalPrice, String thumbnail) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.basePrice = price;
        this.originalPrice = originalPrice;
        this.thumbnail = thumbnail;
        if (originalPrice != null && originalPrice > price) {
            this.discountPercent = (int)((1 - (price / originalPrice)) * 100);
        }
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getPrice() { return price; }
    public Double getBasePrice() { return basePrice; }
    public Double getOriginalPrice() { return originalPrice; }
    public Integer getDiscountPercent() { return discountPercent; }
    public String getThumbnail() { return thumbnail; }
    public List<String> getColorImages() { return colorImages; }
    public List<String> getColorCodes() { return colorCodes; }
    public List<ProductVariant> getVariants() { return variants; }
    public String getSku() { return sku; }
    public String getDescription() { return description; }
    public String getCategoryName() { return categoryName; }
    public List<String> getAllSizes() { return allSizes; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(Double price) { this.price = price; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public void setColorImages(List<String> colorImages) { this.colorImages = colorImages; }
    public void setColorCodes(List<String> colorCodes) { this.colorCodes = colorCodes; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }
    public void setSku(String sku) { this.sku = sku; }
    public void setDescription(String description) { this.description = description; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setAllSizes(List<String> allSizes) { this.allSizes = allSizes; }
}