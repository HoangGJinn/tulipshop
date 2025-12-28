package com.tulip.repository;

import com.tulip.entity.Order;
import com.tulip.entity.OrderItem;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.entity.enums.PaymentStatus;
import com.tulip.entity.product.*;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-Based Test for Reserved Stock Calculation
 * Feature: inventory-management, Property 3: Reserved Stock Calculation
 * Validates: Requirements 1.4
 */
@DataJpaTest
@ActiveProfiles("test")
class OrderItemRepositoryPropertyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderItemRepository orderItemRepository;
    
    private Category category;
    private Product product;
    private ProductVariant variant;
    private Size size;
    private ProductStock stock;
    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
        
        // Setup base test data that will be reused
        category = Category.builder()
                .name("Test Category")
                .build();
        entityManager.persist(category);
        
        product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .basePrice(BigDecimal.valueOf(100.00))
                .category(category)
                .build();
        entityManager.persist(product);
        
        variant = ProductVariant.builder()
                .product(product)
                .colorName("Red")
                .colorCode("#FF0000")
                .build();
        entityManager.persist(variant);
        
        size = Size.builder()
                .code("M")
                .sortOrder(2)
                .build();
        entityManager.persist(size);
        
        stock = ProductStock.builder()
                .variant(variant)
                .size(size)
                .quantity(10000) // Large enough to accommodate all test orders
                .sku("TEST-RED-M")
                .build();
        entityManager.persist(stock);
        
        entityManager.flush();
    }

    /**
     * Property 3: Reserved Stock Calculation
     * For any product stock ID, the calculated reservedStock must equal the sum of quantities 
     * from all OrderItems where the associated Order status is either PENDING or CONFIRMED.
     * 
     * This test runs 100 times with random order statuses and quantities to verify the property holds.
     */
    @RepeatedTest(100)
    @Tag("Feature: inventory-management, Property 3: Reserved Stock Calculation")
    void reservedStockCalculation_shouldEqualSumOfPendingOrderQuantities() {
        // Generate random test data
        int numOrders = random.nextInt(10) + 1; // 1-10 orders
        List<OrderStatus> orderStatuses = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        
        for (int i = 0; i < numOrders; i++) {
            OrderStatus[] allStatuses = OrderStatus.values();
            orderStatuses.add(allStatuses[random.nextInt(allStatuses.length)]);
            quantities.add(random.nextInt(50) + 1); // 1-50 quantity
        }
        
        // Create orders with various statuses
        int expectedReservedStock = 0;
        for (int i = 0; i < orderStatuses.size(); i++) {
            OrderStatus status = orderStatuses.get(i);
            Integer quantity = quantities.get(i);
            
            Order order = Order.builder()
                    .status(status)
                    .paymentMethod(PaymentMethod.COD)
                    .paymentStatus(PaymentStatus.PENDING)
                    .totalPrice(BigDecimal.valueOf(100.00))
                    .shippingPrice(BigDecimal.valueOf(10.00))
                    .finalPrice(BigDecimal.valueOf(110.00))
                    .shippingAddress("Test Address")
                    .orderItems(new ArrayList<>())
                    .build();
            entityManager.persist(order);
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .size(size)
                    .stock(stock)
                    .sku("TEST-RED-M")
                    .quantity(quantity)
                    .priceAtPurchase(BigDecimal.valueOf(100.00))
                    .build();
            entityManager.persist(orderItem);
            
            // Calculate expected reserved stock (only PENDING and CONFIRMED)
            if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
                expectedReservedStock += quantity;
            }
        }
        
        entityManager.flush();
        
        // When: Calculate reserved stock
        Integer actualReservedStock = orderItemRepository.calculateReservedStock(stock.getId());
        
        // Then: Verify the calculation matches expected
        assertThat(actualReservedStock)
                .as("Reserved stock should equal sum of PENDING and CONFIRMED order quantities")
                .isEqualTo(expectedReservedStock);
    }
}
