package com.tulip.repository.specification;

import com.tulip.entity.product.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    /**
     * Lọc theo danh sách category IDs (hỗ trợ N-cấp)
     */
    public static Specification<Product> hasCategoryIds(List<Long> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("category").get("id").in(categoryIds);
        };
    }

    /**
     * Lọc theo tag (sử dụng LIKE)
     */
    public static Specification<Product> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("tags")), "%" + tag.toLowerCase() + "%");
        };
    }

    /**
     * Lọc theo màu (Join với ProductVariant)
     */
    public static Specification<Product> hasColor(String color) {
        return (root, query, cb) -> {
            if (color == null || color.isEmpty()) {
                return cb.conjunction();
            }
            
            // Join với variants
            Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);
            
            // Sử dụng DISTINCT để tránh trùng lặp
            query.distinct(true);
            
            return cb.equal(cb.lower(variantJoin.get("colorName")), color.toLowerCase());
        };
    }

    /**
     * Lọc theo size (Join với ProductVariant và ProductStock)
     * Chỉ lấy sản phẩm có size đó và quantity > 0
     */
    public static Specification<Product> hasSize(String size) {
        return (root, query, cb) -> {
            if (size == null || size.isEmpty()) {
                return cb.conjunction();
            }
            
            // Join với variants
            Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);
            // Join với stocks
            Join<ProductVariant, ProductStock> stockJoin = variantJoin.join("stocks", JoinType.LEFT);
            // Join với size
            Join<ProductStock, Size> sizeJoin = stockJoin.join("size", JoinType.LEFT);
            
            // Sử dụng DISTINCT để tránh trùng lặp
            query.distinct(true);
            
            // Điều kiện: size code khớp VÀ quantity > 0
            return cb.and(
                cb.equal(cb.lower(sizeJoin.get("code")), size.toLowerCase()),
                cb.greaterThan(stockJoin.get("quantity"), 0)
            );
        };
    }

    /**
     * Lọc theo khoảng giá (min - max)
     */
    public static Specification<Product> hasPriceRange(Double minPrice, Double maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), BigDecimal.valueOf(minPrice)));
            }
            
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), BigDecimal.valueOf(maxPrice)));
            }
            
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Lọc theo trạng thái ACTIVE
     */
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    /**
     * Kết hợp tất cả các điều kiện lọc
     */
    public static Specification<Product> buildFilterSpec(
            List<Long> categoryIds,
            String tag,
            String color,
            String size,
            Double minPrice,
            Double maxPrice) {
        
        return Specification.where(isActive())
                .and(hasCategoryIds(categoryIds))
                .and(hasTag(tag))
                .and(hasColor(color))
                .and(hasSize(size))
                .and(hasPriceRange(minPrice, maxPrice));
    }
}
