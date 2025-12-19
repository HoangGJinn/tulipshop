package com.tulip.repository;

import com.tulip.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Tìm item trong giỏ hàng cụ thể với stock cụ thể (để cộng dồn số lượng)
    Optional<CartItem> findByCartIdAndStockId(Long cartId, Long stockId);

    // Xóa tất cả cart items của một cart
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}