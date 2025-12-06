package com.tulip.model; // Đã sửa package cho đúng

import java.util.List;
import java.util.Map;

public class ProductVariant {
    private String colorName;
    private List<String> images;
    private Map<String, Integer> stockBySize;

    public ProductVariant(String colorName, List<String> images, Map<String, Integer> stockBySize) {
        this.colorName = colorName;
        this.images = images;
        this.stockBySize = stockBySize;
    }

    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Map<String, Integer> getStockBySize() { return stockBySize; }
    public void setStockBySize(Map<String, Integer> stockBySize) { this.stockBySize = stockBySize; }
}