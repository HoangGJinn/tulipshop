package com.tulip.service.impl;

import com.tulip.dto.InventoryAlertsDTO;
import com.tulip.dto.InventoryDTO;
import com.tulip.dto.StockHistoryDTO;
import com.tulip.dto.UninitializedVariantDTO;
import com.tulip.dto.request.StockInitRequest;
import com.tulip.entity.enums.StockStatus;
import com.tulip.entity.product.ProductStock;
import com.tulip.entity.product.ProductVariant;
import com.tulip.entity.product.ProductVariantImage;
import com.tulip.entity.product.Size;
import com.tulip.entity.product.StockHistory;
import com.tulip.repository.OrderItemRepository;
import com.tulip.repository.ProductStockRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.repository.StockHistoryRepository;
import com.tulip.repository.VariantRepository;
import com.tulip.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductStockRepository productStockRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final VariantRepository variantRepository;
    private final SizeRepository sizeRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDTO> getInventoryList(String search, String status, Long categoryId) {
        // Fetch all ProductStock records with details
        List<ProductStock> stockRecords = productStockRepository.findAllWithDetails();

        // Convert to DTOs with calculated values
        List<InventoryDTO> inventoryList = stockRecords.stream()
                .map(this::convertToInventoryDTO)
                .collect(Collectors.toList());

        // Apply filters
        return applyFilters(inventoryList, search, status, categoryId);
    }

    @Override
    @Transactional
    public InventoryDTO updatePhysicalStock(Long stockId, Integer newQuantity, String reason) {
        // Acquire pessimistic write lock on the stock record
        ProductStock stock = productStockRepository.findByIdWithLock(stockId)
                .orElseThrow(() -> new RuntimeException("Stock record with ID " + stockId + " not found"));
        
        // Calculate reserved stock
        Integer reservedStock = orderItemRepository.calculateReservedStock(stockId);
        
        // Validate that new quantity doesn't result in negative available stock
        Integer newAvailableStock = newQuantity - reservedStock;
        if (newAvailableStock < 0) {
            throw new IllegalArgumentException(
                    "Cannot set physical stock to " + newQuantity + 
                    ". Reserved stock is " + reservedStock + 
                    ", which would result in negative available stock."
            );
        }
        
        // Store previous quantity for history
        Integer previousQuantity = stock.getQuantity();
        
        // Create stock history record before any changes
        StockHistory history = StockHistory.builder()
                .stock(stock)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .changeAmount(newQuantity - previousQuantity)
                .adminUsername("admin") // TODO: Get from security context
                .reason(reason)
                .build();
        stockHistoryRepository.save(history);
        
        // If new quantity is 0 and no reserved stock, delete the record
        // Otherwise update it (keep for historical data if there's reserved stock)
        if (newQuantity == 0 && reservedStock == 0) {
            productStockRepository.delete(stock);
            // Return a DTO representing the deleted state
            InventoryDTO dto = convertToInventoryDTO(stock);
            dto.setPhysicalStock(0);
            dto.setAvailableStock(0);
            dto.setStatus(StockStatus.OUT_OF_STOCK);
            return dto;
        } else {
            // Update physical stock
            stock.setQuantity(newQuantity);
            productStockRepository.save(stock);
            
            // Return updated inventory DTO
            return convertToInventoryDTO(stock);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockHistoryDTO> getStockHistory(Long stockId) {
        System.out.println("=== Service: Getting stock history for stockId: " + stockId);
        // Fetch history records ordered by timestamp descending
        List<StockHistory> historyRecords = stockHistoryRepository.findByStock_IdOrderByCreatedAtDesc(stockId);
        System.out.println("=== Service: Found " + historyRecords.size() + " history records");
        
        // Map to DTOs
        List<StockHistoryDTO> dtos = historyRecords.stream()
                .map(this::convertToStockHistoryDTO)
                .collect(Collectors.toList());
        System.out.println("=== Service: Converted to " + dtos.size() + " DTOs");
        return dtos;
    }

    @Override
    public byte[] exportToExcel(List<InventoryDTO> inventoryList) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Inventory");
            
            // Create header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Product Name", "SKU", "Color", "Size", "Category",
                "Price", "Physical Stock", "Reserved Stock", "Available Stock", "Status"
            };
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // Create data rows
            int rowNum = 1;
            for (InventoryDTO item : inventoryList) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(item.getProductName());
                row.createCell(1).setCellValue(item.getSku());
                row.createCell(2).setCellValue(item.getColorName());
                row.createCell(3).setCellValue(item.getSizeName());
                row.createCell(4).setCellValue(item.getCategoryName());
                row.createCell(5).setCellValue(item.getPrice().doubleValue());
                row.createCell(6).setCellValue(item.getPhysicalStock());
                row.createCell(7).setCellValue(item.getReservedStock());
                row.createCell(8).setCellValue(item.getAvailableStock());
                row.createCell(9).setCellValue(item.getStatus().name());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to export inventory to Excel", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAlertsDTO getAlerts() {
        // Count variants without stock records
        List<ProductVariant> uninitializedVariants = productStockRepository.findVariantsWithoutStock();
        Integer uninitializedCount = uninitializedVariants.size();
        
        // Count variants with available stock below threshold
        List<InventoryDTO> allInventory = getInventoryList(null, null, null);
        long lowStockCount = allInventory.stream()
                .filter(item -> item.getAvailableStock() > 0 && item.getAvailableStock() <= LOW_STOCK_THRESHOLD)
                .count();
        
        return InventoryAlertsDTO.builder()
                .uninitializedCount(uninitializedCount)
                .lowStockCount((int) lowStockCount)
                .build();
    }

    @Override
    @Transactional
    public int initializeBulkStock(List<StockInitRequest> requests) {
        int initializedCount = 0;
        
        for (StockInitRequest request : requests) {
            // Skip if initial quantity is 0 or negative
            if (request.getInitialQuantity() == null || request.getInitialQuantity() <= 0) {
                continue;
            }
            
            // Check if stock already exists for this variant-size combination
            ProductVariant variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found: " + request.getVariantId()));
            
            com.tulip.entity.product.Size size = sizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Size not found: " + request.getSizeId()));
            
            // Check if stock already exists
            boolean stockExists = variant.getStocks() != null && 
                    variant.getStocks().stream()
                            .anyMatch(s -> s.getSize().getId().equals(request.getSizeId()));
            
            if (!stockExists) {
                // Create new stock record
                String sku = variant.getProduct().getId() + "-" + 
                            variant.getColorName() + "-" + 
                            size.getCode();
                
                ProductStock newStock = ProductStock.builder()
                        .variant(variant)
                        .size(size)
                        .quantity(request.getInitialQuantity())
                        .sku(sku)
                        .build();
                
                productStockRepository.save(newStock);
                initializedCount++;
            }
        }
        
        return initializedCount;
    }

    /**
     * Convert ProductStock entity to InventoryDTO with calculated values
     */
    private InventoryDTO convertToInventoryDTO(ProductStock stock) {
        ProductVariant variant = stock.getVariant();

        // Calculate reserved stock from pending orders
        Integer reservedStock = orderItemRepository.calculateReservedStock(stock.getId());

        // Calculate available stock
        Integer physicalStock = stock.getQuantity();
        Integer availableStock = physicalStock - reservedStock;

        // Determine stock status
        StockStatus stockStatus = determineStockStatus(availableStock);

        // Get image URL (first variant image or placeholder)
        String imageUrl = variant.getImages() != null && !variant.getImages().isEmpty()
                ? variant.getImages().get(0).getImageUrl()
                : "https://placehold.co/600x800?text=No+Image";

        return InventoryDTO.builder()
                .stockId(stock.getId())
                .variantId(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .sku(stock.getSku())
                .imageUrl(imageUrl)
                .colorName(variant.getColorName())
                .sizeName(stock.getSize().getCode())
                .price(variant.getProduct().getDiscountPrice() != null 
                        ? variant.getProduct().getDiscountPrice() 
                        : variant.getProduct().getBasePrice())
                .originalPrice(variant.getProduct().getBasePrice())
                .physicalStock(physicalStock)
                .reservedStock(reservedStock)
                .availableStock(availableStock)
                .status(stockStatus)
                .categoryName(variant.getProduct().getCategory() != null 
                        ? variant.getProduct().getCategory().getName() 
                        : "")
                .categoryId(variant.getProduct().getCategory() != null 
                        ? variant.getProduct().getCategory().getId() 
                        : null)
                .build();
    }

    /**
     * Determine stock status based on available stock quantity
     */
    private StockStatus determineStockStatus(Integer availableStock) {
        if (availableStock <= 0) {
            return StockStatus.OUT_OF_STOCK;
        } else if (availableStock <= LOW_STOCK_THRESHOLD) {
            return StockStatus.LOW_STOCK;
        } else {
            return StockStatus.IN_STOCK;
        }
    }

    /**
     * Apply search and filter criteria to inventory list
     */
    private List<InventoryDTO> applyFilters(List<InventoryDTO> inventoryList, 
                                           String search, 
                                           String status, 
                                           Long categoryId) {
        return inventoryList.stream()
                // Filter by search query (product name or SKU)
                .filter(item -> search == null || search.isEmpty() ||
                        item.getProductName().toLowerCase().contains(search.toLowerCase()) ||
                        item.getSku().toLowerCase().contains(search.toLowerCase()))
                // Filter by stock status
                .filter(item -> status == null || status.isEmpty() ||
                        item.getStatus().name().equals(status))
                // Filter by category ID
                .filter(item -> categoryId == null ||
                        (item.getCategoryId() != null && item.getCategoryId().equals(categoryId)))
                .collect(Collectors.toList());
    }

    /**
     * Convert StockHistory entity to StockHistoryDTO
     */
    private StockHistoryDTO convertToStockHistoryDTO(StockHistory history) {
        return StockHistoryDTO.builder()
                .id(history.getId())
                .stockId(history.getStock().getId())
                .timestamp(history.getCreatedAt())
                .previousQuantity(history.getPreviousQuantity())
                .newQuantity(history.getNewQuantity())
                .changeAmount(history.getChangeAmount())
                .adminUsername(history.getAdminUsername())
                .reason(history.getReason())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UninitializedVariantDTO> getUninitializedVariants() {
        // Get all variant-size combinations that don't have stock records
        List<Object[]> uninitializedCombinations = productStockRepository.findUninitializedVariantSizeCombinations();
        
        return uninitializedCombinations.stream()
                .map(objects -> {
                    ProductVariant variant = (ProductVariant) objects[0];
                    Size size = (Size) objects[1];
                    
                    // Generate SKU
                    String sku = variant.getProduct().getId() + "-" + 
                                variant.getColorName() + "-" + 
                                size.getCode();
                    
                    // Get image URL (first variant image or placeholder)
                    String imageUrl = variant.getImages() != null && !variant.getImages().isEmpty()
                            ? variant.getImages().get(0).getImageUrl()
                            : "https://placehold.co/600x800?text=No+Image";
                    
                    return UninitializedVariantDTO.builder()
                            .variantId(variant.getId())
                            .sizeId(size.getId().longValue())
                            .productName(variant.getProduct().getName())
                            .colorName(variant.getColorName())
                            .sizeName(size.getCode())
                            .sku(sku)
                            .imageUrl(imageUrl)
                            .price(variant.getProduct().getDiscountPrice() != null 
                                    ? variant.getProduct().getDiscountPrice() 
                                    : variant.getProduct().getBasePrice())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
