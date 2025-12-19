package com.tulip.service;

import com.tulip.dto.OrderCreationDTO;
import com.tulip.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(Long userId, OrderCreationDTO request);
    
    List<Order> getUserOrders(Long userId);
    
    Optional<Order> getUserOrder(Long userId, Long orderId);
}