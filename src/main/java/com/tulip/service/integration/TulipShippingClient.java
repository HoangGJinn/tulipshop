package com.tulip.service.integration;

import com.tulip.dto.request.ShippingOrderRequest;
import com.tulip.dto.request.ShippingRateRequest;
import com.tulip.dto.response.ShippingRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TulipShippingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String SHIPPING_URL = "http://localhost:8788/api";

    // Trong file TulipShippingClient.java
    public ShippingRateResponse getShippingFee(String fullAddress, String deliveryType) {
        try {
            String url = SHIPPING_URL + "/shipping-method";
            ShippingRateRequest request = new ShippingRateRequest(fullAddress, deliveryType);

            ShippingRateResponse response = restTemplate.postForObject(url, request, ShippingRateResponse.class);

            if (response != null && response.getShippingFee() != null) {
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi gọi Shipping Service: " + e.getMessage());
        }

        return new ShippingRateResponse(
                "STANDARD",
                new BigDecimal("30000"),
                0.0,
                "3-5 ngày"
        );
    }

    // 2. Gọi API tạo vận đơn (Add Shipping) - Giữ nguyên
    public void createShippingOrder(ShippingOrderRequest orderRequest) {
        try {
            String url = SHIPPING_URL + "/add-shipping";
            restTemplate.postForObject(url, orderRequest, Object.class);
            System.out.println(">> Đã tạo vận đơn bên Tulip Shipping thành công!");
        } catch (Exception e) {
            System.err.println(">> Lỗi khi tạo vận đơn: " + e.getMessage());
        }
    }

    // 3. Gọi API bắt đầu giao hàng (Start Delivery)
    public void startDelivery(String orderCode) {
        try {
            String url = SHIPPING_URL + "/start-delivery/" + orderCode;
            String response = restTemplate.postForObject(url, null, String.class);
            System.out.println(">> Bắt đầu giao hàng: " + response);
        } catch (Exception e) {
            System.err.println(">> Lỗi khi bắt đầu giao hàng: " + e.getMessage());
            throw new RuntimeException("Không thể kích hoạt giao hàng: " + e.getMessage());
        }
    }
}