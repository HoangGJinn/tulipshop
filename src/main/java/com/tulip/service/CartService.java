package com.tulip.service;

import com.tulip.dto.CartItemDTO;

import java.util.List;

public interface CartService {
    // Thêm vào giỏ
    void addToCart(Long userId, Long stockId, int quantity);

    // Lấy danh sách hiển thị
    List<CartItemDTO> getCartItems(Long userId);

    // Lấy danh sách hiển thị (lọc theo IDs)
    List<CartItemDTO> getCartItems(Long userId, List<Long> itemIds);

    // Cập nhật số lượng
    void updateQuantity(Long userId, Long cartItemId, int quantity);

    // Xóa khỏi giỏ
    void removeFromCart(Long userId, Long cartItemId);

    // Đếm tổng sản phẩm (để hiện số trên icon giỏ hàng)
    int countItems(Long userId);

    // Tính tổng tiền tạm tính
    java.math.BigDecimal getTotalPrice(Long userId);

    // Tính tổng tiền tạm tính (lọc theo IDs)
    java.math.BigDecimal getTotalPrice(Long userId, List<Long> itemIds);

    // Xóa toàn bộ giỏ hàng
    void clearCart(Long userId);

    // Xóa các items đã mua khỏi giỏ
    void removeItems(Long userId, List<Long> itemIds);
}