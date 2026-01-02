package com.tulip.service;

import com.tulip.entity.Order;

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
     * @deprecated Use sendOrderUpdateEmail instead
     */
    @Deprecated
    void sendOrderConfirmation(Order order);
}