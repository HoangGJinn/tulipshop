package com.tulip.service;

import com.tulip.entity.Order;
import com.tulip.entity.User;
import com.tulip.entity.product.Product;

public interface EmailService {
    void sendOTPToEmail(String toEmail, String otp, String type);
    
    /**
     * Send order update email based on order status
     * Supports: CONFIRMED, SHIPPING, DELIVERED
     */
    void sendOrderUpdateEmail(Order order);
    
    /**
     * Send rating reminder email after order is delivered
     */
    void sendRatingReminderEmail(Order order);
    
    /**
     * Send wishlist stock alert email
     * @param user User to notify
     * @param product Product that changed stock status
     * @param type Alert type: "LOW_STOCK" or "BACK_IN_STOCK"
     */
    void sendWishlistStockAlert(User user, Product product, String type);
    
    /**
     * @deprecated Use sendOrderUpdateEmail instead
     */
    @Deprecated
    void sendOrderConfirmation(Order order);
}