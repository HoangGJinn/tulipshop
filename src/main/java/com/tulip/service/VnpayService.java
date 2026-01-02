package com.tulip.service;

import com.tulip.dto.request.VnpayRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

public interface VnpayService {
    String createPayment(VnpayRequest paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException;

    String createPaymentWithOrderCode(VnpayRequest paymentRequest, String orderCode, HttpServletRequest request) throws UnsupportedEncodingException;

    // Xử lý kết quả trả về từ VNPAY và cập nhật Order
    // Trả về orderId nếu thành công, null nếu thất bại hoặc không tìm thấy Order
    Long handlePaymentCallback(HttpServletRequest request);
}