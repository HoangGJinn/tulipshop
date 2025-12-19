package com.tulip.service;

import com.tulip.entity.Order;
import com.tulip.entity.OrderItem;
import com.tulip.repository.OrderRepository;
import com.tulip.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationService {

    private final OrderRepository orderRepository;
    private final ProductStockRepository productStockRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelExpiredOrders() {
        log.info("Checking for expired payment orders...");
        
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(
                Order.OrderStatus.PENDING, 
                LocalDateTime.now()
        );
        
        if (expiredOrders.isEmpty()) {
            log.info("No expired orders found");
            return;
        }
        
        log.info("Found {} expired orders, cancelling...", expiredOrders.size());
        
        for (Order order : expiredOrders) {
            try {
                restoreStock(order);
                order.setStatus(com.tulip.entity.Order.OrderStatus.EXPIRED);
                order.setPaymentStatus(com.tulip.entity.PaymentStatus.FAILED);
                orderRepository.save(order);
                log.info("Cancelled expired order: {}", order.getId());
            } catch (Exception e) {
                log.error("Error cancelling expired order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed processing {} expired orders", expiredOrders.size());
    }
    
    private void restoreStock(Order order) {
        if (order.getOrderItems() == null) {
            return;
        }
        
        for (OrderItem item : order.getOrderItems()) {
            if (item.getStock() != null) {
                item.getStock().setQuantity(item.getStock().getQuantity() + item.getQuantity());
                productStockRepository.save(item.getStock());
            }
        }
    }
}

