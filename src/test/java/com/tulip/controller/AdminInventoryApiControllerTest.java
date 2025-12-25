package com.tulip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulip.dto.InventoryAlertsDTO;
import com.tulip.dto.InventoryDTO;
import com.tulip.dto.StockHistoryDTO;
import com.tulip.dto.request.StockInitRequest;
import com.tulip.dto.request.StockUpdateRequest;
import com.tulip.entity.enums.StockStatus;
import com.tulip.service.InventoryService;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AdminInventoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getInventoryList_withAdminRole_returnsInventoryList() throws Exception {
        // Arrange
        List<InventoryDTO> mockInventory = Arrays.asList(
                InventoryDTO.builder()
                        .stockId(1L)
                        .productName("Test Product")
                        .sku("SKU-001")
                        .physicalStock(100)
                        .reservedStock(20)
                        .availableStock(80)
                        .status(StockStatus.IN_STOCK)
                        .build()
        );
        when(inventoryService.getInventoryList(null, null, null)).thenReturn(mockInventory);

        // Act & Assert
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].stockId").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].physicalStock").value(100))
                .andExpect(jsonPath("$[0].reservedStock").value(20))
                .andExpect(jsonPath("$[0].availableStock").value(80));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getInventoryList_withSearchParameter_returnsFilteredList() throws Exception {
        // Arrange
        List<InventoryDTO> mockInventory = Arrays.asList(
                InventoryDTO.builder()
                        .stockId(1L)
                        .productName("Filtered Product")
                        .sku("SKU-001")
                        .build()
        );
        when(inventoryService.getInventoryList("Filtered", null, null)).thenReturn(mockInventory);

        // Act & Assert
        mockMvc.perform(get("/api/admin/inventory")
                        .param("search", "Filtered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Filtered Product"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getInventoryList_withoutAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_withValidRequest_returnsUpdatedInventory() throws Exception {
        // Arrange
        StockUpdateRequest request = StockUpdateRequest.builder()
                .stockId(1L)
                .newPhysicalStock(150)
                .reason("Restock")
                .build();

        InventoryDTO updatedInventory = InventoryDTO.builder()
                .stockId(1L)
                .productName("Test Product")
                .sku("SKU-001")
                .physicalStock(150)
                .reservedStock(20)
                .availableStock(130)
                .status(StockStatus.IN_STOCK)
                .build();

        when(inventoryService.updatePhysicalStock(1L, 150, "Restock"))
                .thenReturn(updatedInventory);

        // Act & Assert
        mockMvc.perform(put("/api/admin/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockId").value(1))
                .andExpect(jsonPath("$.physicalStock").value(150))
                .andExpect(jsonPath("$.availableStock").value(130));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_withNegativeQuantity_returnsBadRequest() throws Exception {
        // Arrange
        StockUpdateRequest request = StockUpdateRequest.builder()
                .stockId(1L)
                .newPhysicalStock(-10)
                .reason("Invalid")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/admin/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Physical stock must be a non-negative number"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_withNegativeAvailableStock_returnsBadRequest() throws Exception {
        // Arrange
        StockUpdateRequest request = StockUpdateRequest.builder()
                .stockId(1L)
                .newPhysicalStock(10)
                .reason("Test")
                .build();

        when(inventoryService.updatePhysicalStock(1L, 10, "Test"))
                .thenThrow(new IllegalArgumentException("Cannot set physical stock to 10. Reserved stock is 20, which would result in negative available stock."));

        // Act & Assert
        mockMvc.perform(put("/api/admin/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_withConcurrentModification_returnsConflict() throws Exception {
        // Arrange
        StockUpdateRequest request = StockUpdateRequest.builder()
                .stockId(1L)
                .newPhysicalStock(100)
                .reason("Test")
                .build();

        when(inventoryService.updatePhysicalStock(1L, 100, "Test"))
                .thenThrow(new PessimisticLockException("Lock timeout"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock record is being modified by another user. Please try again."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStockHistory_withValidStockId_returnsHistory() throws Exception {
        // Arrange
        List<StockHistoryDTO> mockHistory = Arrays.asList(
                StockHistoryDTO.builder()
                        .id(1L)
                        .stockId(1L)
                        .timestamp(LocalDateTime.now())
                        .previousQuantity(100)
                        .newQuantity(150)
                        .changeAmount(50)
                        .adminUsername("admin")
                        .reason("Restock")
                        .build()
        );
        when(inventoryService.getStockHistory(1L)).thenReturn(mockHistory);

        // Act & Assert
        mockMvc.perform(get("/api/admin/inventory/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockId").value(1))
                .andExpect(jsonPath("$[0].previousQuantity").value(100))
                .andExpect(jsonPath("$[0].newQuantity").value(150))
                .andExpect(jsonPath("$[0].changeAmount").value(50))
                .andExpect(jsonPath("$[0].adminUsername").value("admin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlerts_returnsAlertCounts() throws Exception {
        // Arrange
        InventoryAlertsDTO mockAlerts = InventoryAlertsDTO.builder()
                .uninitializedCount(5)
                .lowStockCount(10)
                .build();
        when(inventoryService.getAlerts()).thenReturn(mockAlerts);

        // Act & Assert
        mockMvc.perform(get("/api/admin/inventory/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uninitializedCount").value(5))
                .andExpect(jsonPath("$.lowStockCount").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportToExcel_returnsExcelFile() throws Exception {
        // Arrange
        List<InventoryDTO> mockInventory = Arrays.asList(
                InventoryDTO.builder()
                        .stockId(1L)
                        .productName("Test Product")
                        .sku("SKU-001")
                        .physicalStock(100)
                        .reservedStock(20)
                        .availableStock(80)
                        .build()
        );
        byte[] mockExcelData = "mock excel data".getBytes();
        
        when(inventoryService.getInventoryList(null, null, null)).thenReturn(mockInventory);
        when(inventoryService.exportToExcel(mockInventory)).thenReturn(mockExcelData);

        // Act & Assert
        mockMvc.perform(get("/api/admin/inventory/export"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inventory_")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkInitializeStock_withValidRequests_returnsSuccessCount() throws Exception {
        // Arrange
        List<StockInitRequest> requests = Arrays.asList(
                StockInitRequest.builder()
                        .variantId(1L)
                        .sizeId(1)
                        .initialQuantity(100)
                        .build(),
                StockInitRequest.builder()
                        .variantId(2L)
                        .sizeId(2)
                        .initialQuantity(50)
                        .build()
        );

        when(inventoryService.initializeBulkStock(anyList())).thenReturn(2);

        // Act & Assert
        mockMvc.perform(post("/api/admin/inventory/bulk-init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.message").value("Successfully initialized 2 stock records"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkInitializeStock_withEmptyList_returnsBadRequest() throws Exception {
        // Arrange
        List<StockInitRequest> requests = Arrays.asList();

        // Act & Assert
        mockMvc.perform(post("/api/admin/inventory/bulk-init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request list cannot be empty"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkInitializeStock_withInvalidQuantity_returnsBadRequest() throws Exception {
        // Arrange
        List<StockInitRequest> requests = Arrays.asList(
                StockInitRequest.builder()
                        .variantId(1L)
                        .sizeId(1)
                        .initialQuantity(-10)
                        .build()
        );

        // Act & Assert
        mockMvc.perform(post("/api/admin/inventory/bulk-init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("All fields are required and quantity must be non-negative"));
    }

    @Test
    void updateStock_withoutAuthentication_redirectsToLogin() throws Exception {
        StockUpdateRequest request = StockUpdateRequest.builder()
                .stockId(1L)
                .newPhysicalStock(100)
                .build();

        mockMvc.perform(put("/api/admin/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUninitializedVariants_withAdminRole_returnsUninitializedList() throws Exception {
        // Arrange
        com.tulip.dto.UninitializedVariantDTO variant1 = com.tulip.dto.UninitializedVariantDTO.builder()
                .variantId(1L)
                .sizeId(1L)
                .productName("Test Product")
                .colorName("Red")
                .sizeName("M")
                .sku("TEST-RED-M")
                .imageUrl("http://example.com/image.jpg")
                .price(new BigDecimal("100.00"))
                .build();

        List<com.tulip.dto.UninitializedVariantDTO> mockVariants = Arrays.asList(variant1);
        when(inventoryService.getUninitializedVariants()).thenReturn(mockVariants);

        // Act & Assert
        mockMvc.perform(get("/api/admin/inventory/uninitialized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantId").value(1))
                .andExpect(jsonPath("$[0].sizeId").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].colorName").value("Red"))
                .andExpect(jsonPath("$[0].sizeName").value("M"))
                .andExpect(jsonPath("$[0].sku").value("TEST-RED-M"));
    }
}
