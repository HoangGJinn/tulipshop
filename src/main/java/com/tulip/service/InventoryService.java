package com.tulip.service;

import com.tulip.dto.InventoryAlertsDTO;
import com.tulip.dto.InventoryDTO;
import com.tulip.dto.StockHistoryDTO;
import com.tulip.dto.UninitializedVariantDTO;
import com.tulip.dto.request.StockInitRequest;

import java.util.List;

public interface InventoryService {
    /**
     * Get complete inventory list with optional filters
     * @param search Search query for product name or SKU
     * @param status Stock status filter (IN_STOCK, LOW_STOCK, OUT_OF_STOCK)
     * @param categoryId Category filter
     * @return List of inventory records with calculated stock values
     */
    List<InventoryDTO> getInventoryList(String search, String status, Long categoryId);

    /**
     * Update physical stock quantity for a product variant
     * @param stockId ID of the ProductStock record
     * @param newQuantity New physical stock quantity
     * @param reason Reason for the stock change
     * @return Updated inventory record
     */
    InventoryDTO updatePhysicalStock(Long stockId, Integer newQuantity, String reason);

    /**
     * Get stock change history for a specific stock record
     * @param stockId ID of the ProductStock record
     * @return List of stock history records ordered by timestamp descending
     */
    List<StockHistoryDTO> getStockHistory(Long stockId);

    /**
     * Export inventory data to Excel format
     * @param inventoryList List of inventory records to export
     * @return Excel file as byte array
     */
    byte[] exportToExcel(List<InventoryDTO> inventoryList);

    /**
     * Get inventory alert counts
     * @return Alert counts for uninitialized and low stock items
     */
    InventoryAlertsDTO getAlerts();

    /**
     * Initialize stock records for multiple variants in bulk
     * @param requests List of variant-quantity pairs to initialize
     * @return Count of successfully initialized stock records
     */
    int initializeBulkStock(List<StockInitRequest> requests);

    /**
     * Get list of product variants that don't have stock records initialized
     * @return List of uninitialized variants with their details
     */
    List<UninitializedVariantDTO> getUninitializedVariants();
}
