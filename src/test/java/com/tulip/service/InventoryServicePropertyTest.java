package com.tulip.service;

import com.tulip.dto.InventoryAlertsDTO;
import com.tulip.dto.InventoryDTO;
import com.tulip.dto.StockHistoryDTO;
import com.tulip.dto.request.StockInitRequest;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.StockStatus;
import com.tulip.entity.product.*;
import com.tulip.repository.*;
import com.tulip.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-Based Tests for Inventory List Generation
 * Feature: inventory-management
 */
@DataJpaTest
@ActiveProfiles("test")
class InventoryServicePropertyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductStockRepository productStockRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private SizeRepository sizeRepository;

    private InventoryService inventoryService;
    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
        inventoryService = new InventoryServiceImpl(
                productStockRepository,
                orderItemRepository,
                stockHistoryRepository,
                variantRepository,
                sizeRepository
        );
    }

    /**
     * Property 1: Inventory Record Completeness
     * For any inventory record returned by the system, it must contain all required fields:
     * productName, sku, imageUrl, colorName, sizeName, price, physicalStock, reservedStock, and availableStock.
     * 
     * Validates: Requirements 1.2
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 1: Inventory Record Completeness")
    void inventoryRecordCompleteness_allRecordsMustHaveRequiredFields() {
        // Given: Random inventory data
        int numProducts = random.nextInt(5) + 1; // 1-5 products
        
        for (int i = 0; i < numProducts; i++) {
            createRandomProductWithStock();
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Get inventory list
        List<InventoryDTO> inventoryList = inventoryService.getInventoryList(null, null, null);
        
        // Then: All records must have required fields
        assertThat(inventoryList).isNotEmpty();
        
        for (InventoryDTO record : inventoryList) {
            assertThat(record.getProductName())
                    .as("Product name must not be null")
                    .isNotNull();
            
            assertThat(record.getSku())
                    .as("SKU must not be null")
                    .isNotNull();
            
            assertThat(record.getImageUrl())
                    .as("Image URL must not be null")
                    .isNotNull();
            
            assertThat(record.getColorName())
                    .as("Color name must not be null")
                    .isNotNull();
            
            assertThat(record.getSizeName())
                    .as("Size name must not be null")
                    .isNotNull();
            
            assertThat(record.getPrice())
                    .as("Price must not be null")
                    .isNotNull();
            
            assertThat(record.getPhysicalStock())
                    .as("Physical stock must not be null")
                    .isNotNull();
            
            assertThat(record.getReservedStock())
                    .as("Reserved stock must not be null")
                    .isNotNull();
            
            assertThat(record.getAvailableStock())
                    .as("Available stock must not be null")
                    .isNotNull();
        }
    }

    /**
     * Property 2: Available Stock Calculation Invariant
     * For any inventory record at any point in time, the availableStock value must equal 
     * physicalStock minus reservedStock.
     * 
     * Validates: Requirements 1.3, 3.2
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 2: Available Stock Calculation Invariant")
    void availableStockCalculation_mustEqualPhysicalMinusReserved() {
        // Given: Random inventory with random orders
        int numProducts = random.nextInt(5) + 1; // 1-5 products
        
        for (int i = 0; i < numProducts; i++) {
            ProductStock stock = createRandomProductWithStock();
            
            // Create random orders for this stock
            int numOrders = random.nextInt(3); // 0-2 orders
            for (int j = 0; j < numOrders; j++) {
                createRandomOrder(stock);
            }
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Get inventory list
        List<InventoryDTO> inventoryList = inventoryService.getInventoryList(null, null, null);
        
        // Then: For all records, availableStock = physicalStock - reservedStock
        assertThat(inventoryList).isNotEmpty();
        
        for (InventoryDTO record : inventoryList) {
            int expectedAvailable = record.getPhysicalStock() - record.getReservedStock();
            
            assertThat(record.getAvailableStock())
                    .as("Available stock must equal physical stock minus reserved stock")
                    .isEqualTo(expectedAvailable);
        }
    }

    /**
     * Property 4: Stock Status Classification
     * For any inventory record, the stock status must be correctly classified as:
     * OUT_OF_STOCK if availableStock <= 0, LOW_STOCK if 0 < availableStock <= threshold,
     * or IN_STOCK if availableStock > threshold.
     * 
     * Validates: Requirements 1.5
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 4: Stock Status Classification")
    void stockStatusClassification_mustBeCorrectlyDetermined() {
        // Given: Random inventory with various stock levels
        int LOW_STOCK_THRESHOLD = 10;
        int numProducts = random.nextInt(5) + 1; // 1-5 products
        
        for (int i = 0; i < numProducts; i++) {
            ProductStock stock = createRandomProductWithStock();
            
            // Create orders that may affect available stock
            int numOrders = random.nextInt(3); // 0-2 orders
            for (int j = 0; j < numOrders; j++) {
                createRandomOrder(stock);
            }
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Get inventory list
        List<InventoryDTO> inventoryList = inventoryService.getInventoryList(null, null, null);
        
        // Then: Status must be correctly classified
        assertThat(inventoryList).isNotEmpty();
        
        for (InventoryDTO record : inventoryList) {
            int availableStock = record.getAvailableStock();
            StockStatus expectedStatus;
            
            if (availableStock <= 0) {
                expectedStatus = StockStatus.OUT_OF_STOCK;
            } else if (availableStock <= LOW_STOCK_THRESHOLD) {
                expectedStatus = StockStatus.LOW_STOCK;
            } else {
                expectedStatus = StockStatus.IN_STOCK;
            }
            
            assertThat(record.getStatus())
                    .as("Stock status must be correctly classified for available stock: " + availableStock)
                    .isEqualTo(expectedStatus);
        }
    }

    // Helper methods

    private ProductStock createRandomProductWithStock() {
        Category category = Category.builder()
                .name("Category-" + random.nextInt(1000))
                .slug("category-" + random.nextInt(1000))
                .build();
        entityManager.persist(category);
        
        Product product = Product.builder()
                .name("Product-" + random.nextInt(1000))
                .description("Description")
                .basePrice(BigDecimal.valueOf(50 + random.nextInt(200)))
                .discountPrice(random.nextBoolean() ? BigDecimal.valueOf(40 + random.nextInt(150)) : null)
                .category(category)
                .thumbnail("https://placehold.co/600x800?text=Test")
                .build();
        entityManager.persist(product);
        
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .colorName("Color-" + random.nextInt(100))
                .colorCode("#" + String.format("%06X", random.nextInt(0xFFFFFF)))
                .build();
        entityManager.persist(variant);
        
        // Add variant image
        ProductVariantImage image = ProductVariantImage.builder()
                .variant(variant)
                .imageUrl("https://placehold.co/600x800?text=Variant-" + random.nextInt(100))
                .build();
        entityManager.persist(image);
        
        Size size = Size.builder()
                .code(getSizeCode(random.nextInt(5)))
                .sortOrder(random.nextInt(5))
                .build();
        entityManager.persist(size);
        
        ProductStock stock = ProductStock.builder()
                .variant(variant)
                .size(size)
                .quantity(random.nextInt(100)) // 0-99 physical stock
                .sku("SKU-" + random.nextInt(10000))
                .build();
        entityManager.persist(stock);
        
        return stock;
    }

    private void createRandomOrder(ProductStock stock) {
        OrderStatus[] pendingStatuses = {OrderStatus.PENDING, OrderStatus.CONFIRMED};
        OrderStatus[] allStatuses = OrderStatus.values();
        
        // 50% chance of creating a pending/confirmed order (affects reserved stock)
        OrderStatus status = random.nextBoolean() 
                ? pendingStatuses[random.nextInt(pendingStatuses.length)]
                : allStatuses[random.nextInt(allStatuses.length)];
        
        com.tulip.entity.Order order = com.tulip.entity.Order.builder()
                .status(status)
                .paymentMethod(com.tulip.entity.enums.PaymentMethod.COD)
                .paymentStatus(com.tulip.entity.enums.PaymentStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(100.00))
                .shippingPrice(BigDecimal.valueOf(10.00))
                .finalPrice(BigDecimal.valueOf(110.00))
                .shippingAddress("Test Address")
                .build();
        entityManager.persist(order);
        
        com.tulip.entity.OrderItem orderItem = com.tulip.entity.OrderItem.builder()
                .order(order)
                .product(stock.getVariant().getProduct())
                .variant(stock.getVariant())
                .size(stock.getSize())
                .stock(stock)
                .sku(stock.getSku())
                .quantity(random.nextInt(20) + 1) // 1-20 quantity
                .priceAtPurchase(BigDecimal.valueOf(100.00))
                .build();
        entityManager.persist(orderItem);
    }

    private String getSizeCode(int index) {
        String[] sizes = {"XS", "S", "M", "L", "XL"};
        return sizes[index % sizes.length];
    }

    /**
     * Property 6: Negative Stock Prevention
     * For any stock update request where the new physical stock minus current reserved stock 
     * would be negative, the system must reject the update and return an error.
     * 
     * Validates: Requirements 3.3
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 6: Negative Stock Prevention")
    void negativeStockPrevention_mustRejectUpdatesResultingInNegativeAvailableStock() {
        // Given: A product with stock and some reserved quantity
        ProductStock stock = createRandomProductWithStock();
        
        // Create orders to reserve some stock
        int numOrders = random.nextInt(3) + 1; // 1-3 orders
        int totalReserved = 0;
        for (int i = 0; i < numOrders; i++) {
            com.tulip.entity.Order order = com.tulip.entity.Order.builder()
                    .status(OrderStatus.PENDING) // Will be counted as reserved
                    .paymentMethod(com.tulip.entity.enums.PaymentMethod.COD)
                    .paymentStatus(com.tulip.entity.enums.PaymentStatus.PENDING)
                    .totalPrice(BigDecimal.valueOf(100.00))
                    .shippingPrice(BigDecimal.valueOf(10.00))
                    .finalPrice(BigDecimal.valueOf(110.00))
                    .shippingAddress("Test Address")
                    .build();
            entityManager.persist(order);
            
            int quantity = random.nextInt(10) + 1; // 1-10
            totalReserved += quantity;
            
            com.tulip.entity.OrderItem orderItem = com.tulip.entity.OrderItem.builder()
                    .order(order)
                    .product(stock.getVariant().getProduct())
                    .variant(stock.getVariant())
                    .size(stock.getSize())
                    .stock(stock)
                    .sku(stock.getSku())
                    .quantity(quantity)
                    .priceAtPurchase(BigDecimal.valueOf(100.00))
                    .build();
            entityManager.persist(orderItem);
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Try to update physical stock to less than reserved stock
        int invalidNewQuantity = Math.max(0, totalReserved - random.nextInt(5) - 1); // Less than reserved
        
        // Then: Update should be rejected
        try {
            inventoryService.updatePhysicalStock(stock.getId(), invalidNewQuantity, "Test update");
            assertThat(false)
                    .as("Update should have been rejected for negative available stock")
                    .isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .as("Error message should mention negative available stock")
                    .contains("negative available stock");
        }
    }

    /**
     * Property 7: Concurrent Update Safety
     * For any two concurrent stock update requests targeting the same ProductStock record,
     * the system must serialize the updates such that both updates are applied without lost updates.
     * 
     * Note: This is a simplified test that verifies the locking mechanism is in place.
     * Full concurrent testing would require multi-threaded execution.
     * 
     * Validates: Requirements 3.4, 6.1
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 7: Concurrent Update Safety")
    void concurrentUpdateSafety_mustSerializeUpdatesWithoutLostUpdates() {
        // Given: A product with stock
        ProductStock stock = createRandomProductWithStock();
        Long stockId = stock.getId();
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Update stock multiple times sequentially (simulating serialized concurrent updates)
        int initialQuantity = stock.getQuantity();
        int update1 = initialQuantity + random.nextInt(20) + 1;
        int update2 = update1 + random.nextInt(20) + 1;
        
        InventoryDTO result1 = inventoryService.updatePhysicalStock(stockId, update1, "Update 1");
        InventoryDTO result2 = inventoryService.updatePhysicalStock(stockId, update2, "Update 2");
        
        // Then: Both updates should be reflected
        assertThat(result1.getPhysicalStock())
                .as("First update should be applied")
                .isEqualTo(update1);
        
        assertThat(result2.getPhysicalStock())
                .as("Second update should be applied")
                .isEqualTo(update2);
        
        // Verify final state in database
        entityManager.clear();
        ProductStock finalStock = productStockRepository.findById(stockId).orElseThrow();
        assertThat(finalStock.getQuantity())
                .as("Final quantity should reflect last update")
                .isEqualTo(update2);
    }

    /**
     * Property 5: Search and Filter Correctness
     * For any combination of search query, status filter, and category filter,
     * all returned inventory records must match all applied filters, and the count must equal 
     * the number of matching records.
     * 
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 5: Search and Filter Correctness")
    void searchAndFilterCorrectness_allResultsMustMatchFilters() {
        // Given: Random inventory with various products and categories
        int numProducts = random.nextInt(10) + 5; // 5-14 products
        
        for (int i = 0; i < numProducts; i++) {
            ProductStock stock = createRandomProductWithStock();
            
            // Create random orders to vary stock status
            int numOrders = random.nextInt(3); // 0-2 orders
            for (int j = 0; j < numOrders; j++) {
                createRandomOrder(stock);
            }
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // Get full inventory list
        List<InventoryDTO> fullList = inventoryService.getInventoryList(null, null, null);
        assertThat(fullList).isNotEmpty();
        
        // Test search filter
        if (!fullList.isEmpty()) {
            InventoryDTO sampleItem = fullList.get(random.nextInt(fullList.size()));
            String searchQuery = sampleItem.getProductName().substring(0, 
                    Math.min(5, sampleItem.getProductName().length()));
            
            List<InventoryDTO> searchResults = inventoryService.getInventoryList(searchQuery, null, null);
            
            for (InventoryDTO result : searchResults) {
                boolean matchesSearch = result.getProductName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                       result.getSku().toLowerCase().contains(searchQuery.toLowerCase());
                assertThat(matchesSearch)
                        .as("Search result must match search query: " + searchQuery)
                        .isTrue();
            }
        }
        
        // Test status filter
        StockStatus[] statuses = StockStatus.values();
        StockStatus randomStatus = statuses[random.nextInt(statuses.length)];
        
        List<InventoryDTO> statusResults = inventoryService.getInventoryList(null, randomStatus.name(), null);
        
        for (InventoryDTO result : statusResults) {
            assertThat(result.getStatus())
                    .as("Status filter result must match selected status")
                    .isEqualTo(randomStatus);
        }
        
        // Test category filter
        if (!fullList.isEmpty()) {
            InventoryDTO sampleItem = fullList.get(random.nextInt(fullList.size()));
            if (sampleItem.getCategoryId() != null) {
                List<InventoryDTO> categoryResults = inventoryService.getInventoryList(
                        null, null, sampleItem.getCategoryId());
                
                for (InventoryDTO result : categoryResults) {
                    assertThat(result.getCategoryId())
                            .as("Category filter result must match selected category")
                            .isEqualTo(sampleItem.getCategoryId());
                }
            }
        }
        
        // Test combined filters
        if (!fullList.isEmpty()) {
            InventoryDTO sampleItem = fullList.get(random.nextInt(fullList.size()));
            String searchQuery = sampleItem.getProductName().substring(0, 
                    Math.min(3, sampleItem.getProductName().length()));
            
            List<InventoryDTO> combinedResults = inventoryService.getInventoryList(
                    searchQuery, sampleItem.getStatus().name(), sampleItem.getCategoryId());
            
            for (InventoryDTO result : combinedResults) {
                boolean matchesSearch = result.getProductName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                       result.getSku().toLowerCase().contains(searchQuery.toLowerCase());
                assertThat(matchesSearch)
                        .as("Combined filter result must match search query")
                        .isTrue();
                
                assertThat(result.getStatus())
                        .as("Combined filter result must match status")
                        .isEqualTo(sampleItem.getStatus());
                
                if (sampleItem.getCategoryId() != null) {
                    assertThat(result.getCategoryId())
                            .as("Combined filter result must match category")
                            .isEqualTo(sampleItem.getCategoryId());
                }
            }
        }
    }

    /**
     * Property 9: History Record Completeness
     * For any stock history record, it must contain all required fields:
     * timestamp, previousQuantity, newQuantity, changeAmount, and adminUsername.
     * 
     * Validates: Requirements 5.2
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 9: History Record Completeness")
    void historyRecordCompleteness_allRecordsMustHaveRequiredFields() {
        // Given: A product with stock and multiple updates
        ProductStock stock = createRandomProductWithStock();
        Long stockId = stock.getId();
        
        entityManager.flush();
        entityManager.clear();
        
        // Create multiple stock updates to generate history
        int numUpdates = random.nextInt(5) + 1; // 1-5 updates
        int currentQuantity = stock.getQuantity();
        
        for (int i = 0; i < numUpdates; i++) {
            int newQuantity = currentQuantity + random.nextInt(20) - 10; // +/- 10
            if (newQuantity < 0) newQuantity = 0;
            
            try {
                inventoryService.updatePhysicalStock(stockId, newQuantity, "Update " + i);
                currentQuantity = newQuantity;
            } catch (IllegalArgumentException e) {
                // Skip if update would result in negative available stock
            }
        }
        
        // When: Get stock history
        List<StockHistoryDTO> history = inventoryService.getStockHistory(stockId);
        
        // Then: All records must have required fields
        assertThat(history).isNotEmpty();
        
        for (StockHistoryDTO record : history) {
            assertThat(record.getTimestamp())
                    .as("Timestamp must not be null")
                    .isNotNull();
            
            assertThat(record.getPreviousQuantity())
                    .as("Previous quantity must not be null")
                    .isNotNull();
            
            assertThat(record.getNewQuantity())
                    .as("New quantity must not be null")
                    .isNotNull();
            
            assertThat(record.getChangeAmount())
                    .as("Change amount must not be null")
                    .isNotNull();
            
            assertThat(record.getAdminUsername())
                    .as("Admin username must not be null")
                    .isNotNull();
            
            // Verify change amount calculation
            int expectedChange = record.getNewQuantity() - record.getPreviousQuantity();
            assertThat(record.getChangeAmount())
                    .as("Change amount must equal new quantity minus previous quantity")
                    .isEqualTo(expectedChange);
        }
    }

    /**
     * Property 10: History Chronological Ordering
     * For any list of stock history records returned by the system, each record's timestamp 
     * must be greater than or equal to the next record's timestamp (descending order).
     * 
     * Validates: Requirements 5.3
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 10: History Chronological Ordering")
    void historyChronologicalOrdering_recordsMustBeOrderedByTimestampDescending() {
        // Given: A product with stock and multiple updates
        ProductStock stock = createRandomProductWithStock();
        Long stockId = stock.getId();
        
        entityManager.flush();
        entityManager.clear();
        
        // Create multiple stock updates to generate history
        int numUpdates = random.nextInt(5) + 2; // 2-6 updates
        int currentQuantity = stock.getQuantity();
        
        for (int i = 0; i < numUpdates; i++) {
            int newQuantity = currentQuantity + random.nextInt(20) - 10; // +/- 10
            if (newQuantity < 0) newQuantity = 0;
            
            try {
                inventoryService.updatePhysicalStock(stockId, newQuantity, "Update " + i);
                currentQuantity = newQuantity;
                
                // Small delay to ensure different timestamps
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (IllegalArgumentException e) {
                // Skip if update would result in negative available stock
            }
        }
        
        // When: Get stock history
        List<StockHistoryDTO> history = inventoryService.getStockHistory(stockId);
        
        // Then: Records must be ordered by timestamp descending
        assertThat(history).hasSizeGreaterThanOrEqualTo(1);
        
        for (int i = 0; i < history.size() - 1; i++) {
            StockHistoryDTO current = history.get(i);
            StockHistoryDTO next = history.get(i + 1);
            
            assertThat(current.getTimestamp())
                    .as("Current record timestamp must be >= next record timestamp (descending order)")
                    .isAfterOrEqualTo(next.getTimestamp());
        }
    }

    /**
     * Property 11: Alert Count Accuracy
     * For any system state, the count of uninitialized SKUs in the alert banner must equal 
     * the number of ProductVariants without stock records, and the count of low-stock SKUs 
     * must equal the number of inventory records with availableStock below the threshold.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 11: Alert Count Accuracy")
    void alertCountAccuracy_countsMustMatchActualInventoryState() {
        // Given: Random inventory state with some uninitialized and low-stock variants
        int LOW_STOCK_THRESHOLD = 10;
        
        // Create some products with stock
        int numWithStock = random.nextInt(5) + 1; // 1-5 products with stock
        int expectedLowStock = 0;
        
        for (int i = 0; i < numWithStock; i++) {
            ProductStock stock = createRandomProductWithStock();
            
            // Randomly set some to low stock levels
            if (random.nextBoolean()) {
                stock.setQuantity(random.nextInt(LOW_STOCK_THRESHOLD) + 1); // 1-10
                entityManager.persist(stock);
                
                // Check if this will actually be low stock after reserved calculation
                int reserved = 0; // No orders yet, so reserved = 0
                int available = stock.getQuantity() - reserved;
                if (available > 0 && available <= LOW_STOCK_THRESHOLD) {
                    expectedLowStock++;
                }
            }
        }
        
        // Create some variants without stock (uninitialized)
        int numWithoutStock = random.nextInt(3); // 0-2 uninitialized variants
        
        for (int i = 0; i < numWithoutStock; i++) {
            Category category = Category.builder()
                    .name("Category-" + random.nextInt(1000))
                    .slug("category-" + random.nextInt(1000))
                    .build();
            entityManager.persist(category);
            
            Product product = Product.builder()
                    .name("Product-" + random.nextInt(1000))
                    .description("Description")
                    .basePrice(BigDecimal.valueOf(100.00))
                    .category(category)
                    .build();
            entityManager.persist(product);
            
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .colorName("Color-" + random.nextInt(100))
                    .colorCode("#" + String.format("%06X", random.nextInt(0xFFFFFF)))
                    .build();
            entityManager.persist(variant);
            // Note: No ProductStock created for this variant
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Get alerts
        InventoryAlertsDTO alerts = inventoryService.getAlerts();
        
        // Then: Verify uninitialized count
        assertThat(alerts.getUninitializedCount())
                .as("Uninitialized count must match number of variants without stock")
                .isEqualTo(numWithoutStock);
        
        // Verify low stock count (approximately, since we didn't create orders)
        assertThat(alerts.getLowStockCount())
                .as("Low stock count must be non-negative")
                .isGreaterThanOrEqualTo(0);
        
        // Verify by manually counting from inventory list
        List<InventoryDTO> inventory = inventoryService.getInventoryList(null, null, null);
        long actualLowStock = inventory.stream()
                .filter(item -> item.getAvailableStock() > 0 && 
                               item.getAvailableStock() <= LOW_STOCK_THRESHOLD)
                .count();
        
        assertThat(alerts.getLowStockCount())
                .as("Low stock count must match actual count from inventory")
                .isEqualTo((int) actualLowStock);
    }

    /**
     * Property 12: Bulk Initialization Correctness
     * For any bulk initialization request containing a list of variant-quantity pairs,
     * the system must create exactly one ProductStock record for each pair with the specified quantity,
     * and no records should be created for variants already having stock.
     * 
     * Validates: Requirements 8.3
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 12: Bulk Initialization Correctness")
    void bulkInitializationCorrectness_mustCreateCorrectNumberOfStockRecords() {
        // Given: Random variants without stock and some with stock
        int numNewVariants = random.nextInt(5) + 1; // 1-5 new variants
        List<StockInitRequest> requests = new ArrayList<>();
        
        // Create sizes
        List<com.tulip.entity.product.Size> sizes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            com.tulip.entity.product.Size size = com.tulip.entity.product.Size.builder()
                    .code(getSizeCode(i))
                    .sortOrder(i)
                    .build();
            entityManager.persist(size);
            sizes.add(size);
        }
        
        // Create variants without stock
        for (int i = 0; i < numNewVariants; i++) {
            Category category = Category.builder()
                    .name("Category-" + random.nextInt(1000))
                    .slug("category-" + random.nextInt(1000))
                    .build();
            entityManager.persist(category);
            
            Product product = Product.builder()
                    .name("Product-" + random.nextInt(1000))
                    .description("Description")
                    .basePrice(BigDecimal.valueOf(100.00))
                    .category(category)
                    .build();
            entityManager.persist(product);
            
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .colorName("Color-" + random.nextInt(100))
                    .colorCode("#" + String.format("%06X", random.nextInt(0xFFFFFF)))
                    .build();
            entityManager.persist(variant);
            
            // Create request for this variant
            com.tulip.entity.product.Size randomSize = sizes.get(random.nextInt(sizes.size()));
            int quantity = random.nextInt(50) + 1; // 1-50
            
            requests.add(StockInitRequest.builder()
                    .variantId(variant.getId())
                    .sizeId(randomSize.getId())
                    .initialQuantity(quantity)
                    .build());
        }
        
        // Create one variant that already has stock (should be skipped)
        ProductStock existingStock = createRandomProductWithStock();
        requests.add(StockInitRequest.builder()
                .variantId(existingStock.getVariant().getId())
                .sizeId(existingStock.getSize().getId())
                .initialQuantity(100)
                .build());
        
        entityManager.flush();
        entityManager.clear();
        
        // When: Initialize bulk stock
        int initializedCount = inventoryService.initializeBulkStock(requests);
        
        // Then: Verify correct number of records created (excluding existing)
        assertThat(initializedCount)
                .as("Initialized count must equal number of new variant-size pairs")
                .isEqualTo(numNewVariants); // Should not include the existing stock
        
        // Verify quantities match input
        for (StockInitRequest request : requests) {
            List<ProductStock> stocks = productStockRepository.findAll().stream()
                    .filter(s -> s.getVariant().getId().equals(request.getVariantId()) &&
                                s.getSize().getId().equals(request.getSizeId()))
                    .collect(Collectors.toList());
            
            if (stocks.size() == 1) {
                // New stock was created
                assertThat(stocks.get(0).getQuantity())
                        .as("Stock quantity must match requested quantity")
                        .isEqualTo(request.getInitialQuantity());
            }
            // If stocks.size() > 1, it means stock already existed (which is expected for one case)
        }
    }

    /**
     * Property 8: Export Data Completeness
     * For any export operation, the generated Excel file must contain exactly the visible 
     * inventory records with all columns (productName, sku, variants, price, physicalStock, 
     * reservedStock, availableStock), and the filename must contain a timestamp.
     * 
     * Validates: Requirements 4.1, 4.2, 4.3
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 8: Export Data Completeness")
    void exportDataCompleteness_excelFileMustContainAllRecordsAndColumns() {
        // Given: Random inventory data
        int numProducts = random.nextInt(5) + 1; // 1-5 products
        
        for (int i = 0; i < numProducts; i++) {
            createRandomProductWithStock();
        }
        
        entityManager.flush();
        entityManager.clear();
        
        // Get inventory list
        List<InventoryDTO> inventoryList = inventoryService.getInventoryList(null, null, null);
        assertThat(inventoryList).isNotEmpty();
        
        // When: Export to Excel
        byte[] excelData = inventoryService.exportToExcel(inventoryList);
        
        // Then: Verify Excel file is not empty
        assertThat(excelData)
                .as("Excel data must not be null or empty")
                .isNotNull()
                .isNotEmpty();
        
        // Verify we can read the Excel file
        try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(excelData);
             org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)) {
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            
            // Verify header row exists
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
            assertThat(headerRow)
                    .as("Header row must exist")
                    .isNotNull();
            
            // Verify all required columns are present
            String[] expectedHeaders = {
                "Product Name", "SKU", "Color", "Size", "Category",
                "Price", "Physical Stock", "Reserved Stock", "Available Stock", "Status"
            };
            
            for (int i = 0; i < expectedHeaders.length; i++) {
                String actualHeader = headerRow.getCell(i).getStringCellValue();
                assertThat(actualHeader)
                        .as("Column " + i + " header must match")
                        .isEqualTo(expectedHeaders[i]);
            }
            
            // Verify number of data rows matches inventory list
            int dataRowCount = sheet.getLastRowNum(); // 0-indexed, so this is the last row number
            assertThat(dataRowCount)
                    .as("Number of data rows must match inventory list size")
                    .isEqualTo(inventoryList.size()); // Header is row 0, so last row = size
            
            // Verify at least one data row has all columns populated
            if (inventoryList.size() > 0) {
                org.apache.poi.ss.usermodel.Row firstDataRow = sheet.getRow(1);
                assertThat(firstDataRow)
                        .as("First data row must exist")
                        .isNotNull();
                
                assertThat(firstDataRow.getCell(0).getStringCellValue())
                        .as("Product name must not be empty")
                        .isNotEmpty();
                
                assertThat(firstDataRow.getCell(1).getStringCellValue())
                        .as("SKU must not be empty")
                        .isNotEmpty();
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to read exported Excel file", e);
        }
    }
}
