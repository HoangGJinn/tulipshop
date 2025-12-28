package com.tulip.repository;

import com.tulip.entity.product.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductStockRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductStockRepository productStockRepository;

    private Product product;
    private ProductVariant variant;
    private Size size;
    private Category category;

    @BeforeEach
    void setUp() {
        // Create category
        category = Category.builder()
                .name("Test Category")
                .build();
        entityManager.persist(category);

        // Create product
        product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .basePrice(BigDecimal.valueOf(100.00))
                .category(category)
                .build();
        entityManager.persist(product);

        // Create variant
        variant = ProductVariant.builder()
                .product(product)
                .colorName("Red")
                .colorCode("#FF0000")
                .build();
        entityManager.persist(variant);

        // Create size
        size = Size.builder()
                .code("M")
                .sortOrder(2)
                .build();
        entityManager.persist(size);

        entityManager.flush();
    }

    @Test
    void findAllWithDetails_shouldReturnCompleteData() {
        // Given
        ProductStock stock = ProductStock.builder()
                .variant(variant)
                .size(size)
                .quantity(10)
                .sku("TEST-RED-M")
                .build();
        entityManager.persist(stock);
        entityManager.flush();
        entityManager.clear();

        // When
        List<ProductStock> results = productStockRepository.findAllWithDetails();

        // Then
        assertThat(results).isNotEmpty();
        ProductStock result = results.get(0);
        assertThat(result.getVariant()).isNotNull();
        assertThat(result.getVariant().getProduct()).isNotNull();
        assertThat(result.getSize()).isNotNull();
        assertThat(result.getVariant().getProduct().getName()).isEqualTo("Test Product");
        assertThat(result.getVariant().getColorName()).isEqualTo("Red");
        assertThat(result.getSize().getCode()).isEqualTo("M");
    }

    @Test
    void findByIdWithLock_shouldAcquireLock() {
        // Given
        ProductStock stock = ProductStock.builder()
                .variant(variant)
                .size(size)
                .quantity(10)
                .sku("TEST-RED-M")
                .build();
        entityManager.persist(stock);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<ProductStock> result = productStockRepository.findByIdWithLock(stock.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(stock.getId());
        assertThat(result.get().getSku()).isEqualTo("TEST-RED-M");
    }

    @Test
    void findVariantsWithoutStock_shouldReturnCorrectVariants() {
        // Given - variant already exists from setUp, but no stock created
        ProductVariant variantWithStock = ProductVariant.builder()
                .product(product)
                .colorName("Blue")
                .colorCode("#0000FF")
                .build();
        entityManager.persist(variantWithStock);

        ProductStock stock = ProductStock.builder()
                .variant(variantWithStock)
                .size(size)
                .quantity(5)
                .sku("TEST-BLUE-M")
                .build();
        entityManager.persist(stock);
        entityManager.flush();
        entityManager.clear();

        // When
        List<ProductVariant> results = productStockRepository.findVariantsWithoutStock();

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(v -> v.getColorName().equals("Red"));
        assertThat(results).noneMatch(v -> v.getColorName().equals("Blue"));
    }
}
