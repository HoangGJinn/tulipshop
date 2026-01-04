package com.tulip.controller.api;

import com.tulip.dto.InventoryAlertsDTO;
import com.tulip.dto.InventoryDTO;
import com.tulip.dto.StockHistoryDTO;
import com.tulip.dto.UninitializedVariantDTO;
import com.tulip.dto.request.StockInitRequest;
import com.tulip.dto.request.StockUpdateRequest;
import com.tulip.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminInventoryApiController {

    private final InventoryService inventoryService;

    /**
     * Get inventory list with optional search and filter parameters
     * GET /api/admin/inventory?search=...&status=...&categoryId=...
     */
    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getInventoryList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId
    ) {
        try {
            List<InventoryDTO> inventoryList = inventoryService.getInventoryList(search, status, categoryId);
            return ResponseEntity.ok(inventoryList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update physical stock for a specific stock record
     * PUT /api/admin/inventory/{stockId}
     */
    @PutMapping("/{stockId}")
    public ResponseEntity<?> updateStock(
            @PathVariable Long stockId,
            @RequestBody StockUpdateRequest request
    ) {
        try {
            // Validate request
            if (request.getNewPhysicalStock() == null || request.getNewPhysicalStock() < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Physical stock must be a non-negative number"));
            }

            InventoryDTO updated = inventoryService.updatePhysicalStock(
                    stockId,
                    request.getNewPhysicalStock(),
                    request.getReason()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            // Negative available stock or validation error
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (jakarta.persistence.OptimisticLockException | jakarta.persistence.PessimisticLockException e) {
            // Concurrent modification
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Stock record is being modified by another user. Please try again."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update stock: " + e.getMessage()));
        }
    }

    /**
     * Get stock change history for a specific stock record
     * GET /api/admin/inventory/{stockId}/history
     */
    @GetMapping("/{stockId}/history")
    public ResponseEntity<List<StockHistoryDTO>> getStockHistory(@PathVariable Long stockId) {
        try {
            System.out.println("=== Getting stock history for stockId: " + stockId);
            List<StockHistoryDTO> history = inventoryService.getStockHistory(stockId);
            System.out.println("=== Found " + history.size() + " history records");
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            System.err.println("=== Error getting stock history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get inventory alerts (uninitialized and low stock counts)
     * GET /api/admin/inventory/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<InventoryAlertsDTO> getAlerts() {
        try {
            InventoryAlertsDTO alerts = inventoryService.getAlerts();
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export inventory data to Excel
     * GET /api/admin/inventory/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId
    ) {
        try {
            // Get filtered inventory list
            List<InventoryDTO> inventoryList = inventoryService.getInventoryList(search, status, categoryId);
            
            // Generate Excel file
            byte[] excelData = inventoryService.exportToExcel(inventoryList);
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "inventory_" + timestamp + ".xlsx";

            // Use ContentDisposition builder to properly format the header
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(filename)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Initialize stock records for multiple variants in bulk
     * POST /api/admin/inventory/bulk-init
     */
    @PostMapping("/bulk-init")
    public ResponseEntity<?> bulkInitializeStock(@RequestBody List<StockInitRequest> requests) {
        try {
            // Validate requests
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Request list cannot be empty"));
            }
            
            for (StockInitRequest request : requests) {
                if (request.getVariantId() == null || request.getSizeId() == null || 
                    request.getInitialQuantity() == null || request.getInitialQuantity() < 0) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "All fields are required and quantity must be non-negative"));
                }
            }
            
            int initializedCount = inventoryService.initializeBulkStock(requests);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Successfully initialized " + initializedCount + " stock records",
                    "count", initializedCount
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to initialize stock: " + e.getMessage()));
        }
    }

    /**
     * Get list of uninitialized variants (variants without stock records)
     * GET /api/admin/inventory/uninitialized
     */
    @GetMapping("/uninitialized")
    public ResponseEntity<List<UninitializedVariantDTO>> getUninitializedVariants() {
        try {
            List<UninitializedVariantDTO> uninitializedVariants = inventoryService.getUninitializedVariants();
            return ResponseEntity.ok(uninitializedVariants);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
