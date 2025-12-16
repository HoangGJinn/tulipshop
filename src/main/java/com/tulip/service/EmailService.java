package com.tulip.service;

public interface EmailService {
    void sendOTPToEmail(String toEmail, String otp);
}