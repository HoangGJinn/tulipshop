package com.tulip.service;

import com.tulip.entity.Order;

public interface EmailService {
    void sendOTPToEmail(String toEmail, String otp, String type);
    void sendOrderConfirmation(Order order);
}