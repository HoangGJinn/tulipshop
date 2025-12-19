package com.tulip.service;

import com.tulip.dto.CartItemDTO;

import java.util.List;

public interface CartService {
    // Thêm vào giỏ
    void addToCart(Long userId, Long stockId, int quantity);

    // Lấy danh sách hiển thị
    List<CartItemDTO> getCartItems(Long userId);

    // Cập nhật số lượng
    void updateQuantity(Long userId, Long cartItemId, int quantity);

    // Xóa khỏi giỏ
    void removeFromCart(Long userId, Long cartItemId);

    // Đếm tổng sản phẩm (để hiện số trên icon giỏ hàng)
    int countItems(Long userId);

    // Tính tổng tiền tạm tính
    java.math.BigDecimal getTotalPrice(Long userId);

    // Xóa toàn bộ giỏ hàng
    void clearCart(Long userId);
}