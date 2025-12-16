package com.tulip.repository;

import com.tulip.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Tìm item trong giỏ hàng cụ thể với stock cụ thể (để cộng dồn số lượng)
    Optional<CartItem> findByCartIdAndStockId(Long cartId, Long stockId);

    void deleteByCartId(Long cartId);
    // Trong CartItemRepository.java
    void deleteAllByCartId(Long cartId);// Dùng để xóa sạch giỏ hàng sau khi mua
}