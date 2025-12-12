package com.tulip.repository;

import com.tulip.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    // Query với JOIN FETCH để load cartItems
    // Các quan hệ khác (stock, variant, product, size) sẽ được lazy load trong transaction
    @Query("SELECT DISTINCT c FROM Cart c " +
           "LEFT JOIN FETCH c.cartItems " +
           "WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    // Kiểm tra xem user đã có giỏ hàng chưa
    boolean existsByUserId(Long userId);
}