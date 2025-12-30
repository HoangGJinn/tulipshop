package com.tulip.controller;

import com.tulip.entity.Order;
import com.tulip.repository.OrderRepository;
import com.tulip.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/email")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {
    
    private final EmailService emailService;
    private final OrderRepository orderRepository;
    
    @GetMapping("/send-order-confirmation/{orderId}")
    public String testOrderConfirmationEmail(@PathVariable Long orderId) {
        try {
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
            
            // Eager load relationships
            Hibernate.initialize(order.getUser());
            if (order.getUser().getProfile() != null) {
                Hibernate.initialize(order.getUser().getProfile());
            }
            Hibernate.initialize(order.getOrderItems());
            order.getOrderItems().forEach(item -> {
                if (item.getProduct() != null) {
                    Hibernate.initialize(item.getProduct());
                }
                if (item.getVariant() != null) {
                    Hibernate.initialize(item.getVariant());
                    Hibernate.initialize(item.getVariant().getImages());
                }
                if (item.getSize() != null) {
                    Hibernate.initialize(item.getSize());
                }
            });

            emailService.sendOrderUpdateEmail(order);

            return String.format(
                "✅ Email test initiated for order #%d (Status: %s) to %s. Check console logs for details.",
                orderId,
                order.getStatus(),
                order.getUser().getEmail()
            );
        } catch (Exception e) {
            log.error("🧪 [TEST] Error testing email: {}", e.getMessage(), e);
            return "❌ Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/send-order-update/{orderId}")
    public String testOrderUpdateEmail(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
            
            // Eager load relationships
            Hibernate.initialize(order.getUser());
            if (order.getUser().getProfile() != null) {
                Hibernate.initialize(order.getUser().getProfile());
            }
            Hibernate.initialize(order.getOrderItems());
            order.getOrderItems().forEach(item -> {
                if (item.getProduct() != null) {
                    Hibernate.initialize(item.getProduct());
                }
                if (item.getVariant() != null) {
                    Hibernate.initialize(item.getVariant());
                    Hibernate.initialize(item.getVariant().getImages());
                }
                if (item.getSize() != null) {
                    Hibernate.initialize(item.getSize());
                }
            });

            emailService.sendOrderUpdateEmail(order);

            return String.format(
                "✅ Order update email sent for order #%d (Status: %s) to %s. Check console logs for details.",
                orderId,
                order.getStatus(),
                order.getUser().getEmail()
            );
        } catch (Exception e) {
            log.error("🧪 [TEST] Error testing email: {}", e.getMessage(), e);
            return "❌ Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/check-order/{orderId}")
    public String checkOrder(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Hibernate.initialize(order.getUser());
            if (order.getUser().getProfile() != null) {
                Hibernate.initialize(order.getUser().getProfile());
            }
            Hibernate.initialize(order.getOrderItems());
            
            StringBuilder info = new StringBuilder();
            info.append("Order #").append(order.getId()).append("\n");
            info.append("User ID: ").append(order.getUser().getId()).append("\n");
            info.append("User Email: ").append(order.getUser().getEmail()).append("\n");
            info.append("User Name: ").append(
                order.getUser().getProfile() != null ? 
                order.getUser().getProfile().getFullName() : "N/A"
            ).append("\n");
            info.append("Order Items: ").append(order.getOrderItems().size()).append("\n");
            info.append("Total Price: ").append(order.getFinalPrice()).append("\n");
            info.append("Status: ").append(order.getStatus()).append("\n");
            info.append("Payment Method: ").append(order.getPaymentMethod()).append("\n");
            
            return info.toString();
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
}
