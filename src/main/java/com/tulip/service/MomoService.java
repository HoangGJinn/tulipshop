package com.tulip.service;

import java.util.Map;

public interface MomoService {
    String createPaymentRequest(String amount, String orderId, String orderInfo, String requestType);

    String checkPaymentStatus(String orderId);

    void processCallback(Map<String, Object> callbackData);
}

