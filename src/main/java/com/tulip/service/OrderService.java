package com.tulip.service;

import com.tulip.dto.OrderCreationDTO;
import com.tulip.entity.Order;

public interface OrderService {
    Order placeOrder(Long userId, OrderCreationDTO request);
}