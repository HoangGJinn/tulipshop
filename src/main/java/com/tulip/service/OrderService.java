package com.tulip.service;

import com.tulip.dto.OrderCreationDTO;
import com.tulip.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(Long userId, OrderCreationDTO request);
    
    List<Order> getUserOrders(Long userId);
    
    Optional<Order> getUserOrder(Long userId, Long orderId);
    
    /**
     * Mua lại đơn hàng đã hết hạn - thêm sản phẩm vào giỏ hàng
     * @param userId ID người dùng
     * @param orderId ID đơn hàng cũ (phải là EXPIRED)
     * @throws RuntimeException nếu đơn hàng không hợp lệ hoặc không đủ tồn kho
     */
    void reOrderToCart(Long userId, Long orderId);

    void confirmOrderPayment(Long orderId);

    void handlePaymentFailure(Long orderId);
}