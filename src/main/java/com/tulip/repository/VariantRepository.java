package com.tulip.repository;

import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantRepository extends JpaRepository<ProductVariant, Long> {


}
