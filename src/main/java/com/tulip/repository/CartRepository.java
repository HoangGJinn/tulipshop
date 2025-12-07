package com.tulip.repository;

import com.tulip.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    // Kiểm tra xem user đã có giỏ hàng chưa
    boolean existsByUserId(Long userId);
}