package com.tulip.service.impl;

import com.tulip.config.payment.MomoConfig;
import com.tulip.dto.request.CreateMomoRequest;
import com.tulip.dto.response.CreateMomoResponse;
import com.tulip.entity.Order;
import com.tulip.entity.enums.PaymentStatus;
import com.tulip.repository.OrderRepository;
import com.tulip.service.MomoService;
import com.tulip.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class MomoServiceImpl implements MomoService {

    @Autowired
    private MomoConfig momoConfig;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String MOMO_CREATE_PAYMENT_URL = "https://test-payment.momo.vn/v2/gateway/api/create";
    private static final String MOMO_QUERY_STATUS_URL = "https://test-payment.momo.vn/v2/gateway/api/query";
    @Autowired
    private OrderService orderService;

    @Override
    public String createPaymentRequest(String amount, String orderId, String orderInfo, String requestType) {
        try {
            String requestId = momoConfig.getPartnerCode() + new Date().getTime();
            String finalOrderId = (orderId != null && !orderId.isEmpty()) ? orderId : requestId;
            String finalOrderInfo = (orderInfo != null && !orderInfo.isEmpty()) ? orderInfo : "Thanh toan don hang: " + finalOrderId;
            String extraData = "";
            String finalRequestType = (requestType != null && !requestType.isEmpty()) 
                    ? requestType 
                    : momoConfig.getRequestType();

            int expireAfterMinutes = 15;
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    momoConfig.getAccessKey(), amount, extraData, momoConfig.getIpnUrl(), 
                    finalOrderId, finalOrderInfo, momoConfig.getPartnerCode(), 
                    momoConfig.getReturnUrl(), requestId, finalRequestType);

            String signature = signHmacSHA256(rawSignature, momoConfig.getSecretKey());
            log.info("Generated Signature: {}", signature);

            CreateMomoRequest request = CreateMomoRequest.builder()
                    .partnerCode(momoConfig.getPartnerCode())
                    .requestId(requestId)
                    .amount(Long.parseLong(amount))
                    .orderId(finalOrderId)
                    .orderInfo(finalOrderInfo)
                    .redirectUrl(momoConfig.getReturnUrl())
                    .ipnUrl(momoConfig.getIpnUrl())
                    .extraData(extraData)
                    .requestType(finalRequestType)
                    .signature(signature)
                    .lang("vi")
                    .expireAfter(expireAfterMinutes)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateMomoRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<CreateMomoResponse> response = restTemplate.postForEntity(
                    MOMO_CREATE_PAYMENT_URL, entity, CreateMomoResponse.class);

            CreateMomoResponse momoResponse = response.getBody();
            if (momoResponse != null && momoResponse.getResultCode() == 0) {
                log.info("MoMo payment URL created successfully: {}", momoResponse.getPayUrl());
                return momoResponse.getPayUrl();
            } else {
                String errorMsg = momoResponse != null ? momoResponse.getMessage() : "Unknown error";
                log.error("Failed to create MoMo payment: {}", errorMsg);
                throw new RuntimeException("Failed to create MoMo payment: " + errorMsg);
            }

        } catch (Exception e) {
            log.error("Error creating MoMo payment request", e);
            throw new RuntimeException("Failed to create payment request: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkPaymentStatus(String orderId) {
        try {
            String requestId = momoConfig.getPartnerCode() + new Date().getTime();
            String rawSignature = String.format(
                    "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                    momoConfig.getAccessKey(), orderId, momoConfig.getPartnerCode(), requestId);

            String signature = signHmacSHA256(rawSignature, momoConfig.getSecretKey());
            log.info("Generated Signature for Status Check: {}", signature);

            Map<String, Object> requestBody = Map.of(
                    "partnerCode", momoConfig.getPartnerCode(),
                    "accessKey", momoConfig.getAccessKey(),
                    "requestId", requestId,
                    "orderId", orderId,
                    "signature", signature,
                    "lang", "vi"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    MOMO_QUERY_STATUS_URL, entity, String.class);

            log.info("Response from MoMo (Status Check): {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Error checking MoMo payment status", e);
            return "{\"error\": \"Failed to check payment status: " + e.getMessage() + "\"}";
        }
    }

    @Override
    @Transactional
    public void processCallback(Map<String, Object> callbackData) {
        try {
            log.info("Processing MoMo callback: {}", callbackData);

            if (!verifySignature(callbackData)) {
                log.error("Invalid signature in MoMo callback");
                return;
            }

            Integer resultCode = (Integer) callbackData.get("resultCode");
            String orderId = (String) callbackData.get("orderId");
            String transactionId = (String) callbackData.get("transId");

            if (orderId == null) {
                log.error("OrderId is null in MoMo callback");
                return;
            }

            Order order = null;
            Long extractedOrderId = com.tulip.util.VnpayUtil.extractOrderIdFromVnpTxnRef(orderId);
            if (extractedOrderId != null) {
                order = orderRepository.findById(extractedOrderId).orElse(null);
            }
            
            if (order == null) {
                order = orderRepository.findByVnpTxnRef(orderId);
            }
            
            if (order == null) {
                try {
                    Long orderIdLong = Long.parseLong(orderId);
                    order = orderRepository.findById(orderIdLong).orElse(null);
                } catch (NumberFormatException e) {
                    log.warn("OrderId is not a number and not found by vnpTxnRef: {}", orderId);
                }
            }

            if (order == null) {
                log.error("Order not found for orderId: {}", orderId);
                return;
            }

            if (resultCode != null && resultCode == 0) {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                if (transactionId != null) {
                    order.setTransactionId(transactionId);
                }
                log.info("Payment successful for order: {}", orderId);

            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                log.warn("Payment failed for order: {}, resultCode: {}", orderId, resultCode);
            }

            orderRepository.save(order);

        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
        }
    }

    private boolean verifySignature(Map<String, Object> callbackData) {
        try {
            String receivedSignature = (String) callbackData.get("signature");
            if (receivedSignature == null) {
                return false;
            }

            StringBuilder rawSignature = new StringBuilder();
            String[] fields = {"accessKey", "amount", "extraData", "message", "orderId", 
                              "orderInfo", "orderType", "partnerCode", "payType", 
                              "requestId", "responseTime", "resultCode", "transId"};
            
            for (String field : fields) {
                Object value = callbackData.get(field);
                if (value != null) {
                    if (rawSignature.length() > 0) {
                        rawSignature.append("&");
                    }
                    rawSignature.append(field).append("=").append(value);
                }
            }

            String calculatedSignature = signHmacSHA256(rawSignature.toString(), momoConfig.getSecretKey());
            return calculatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Error verifying MoMo signature", e);
            return false;
        }
    }

    private String signHmacSHA256(String data, String key) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256.init(secretKey);
            byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error signing HMAC SHA256", e);
            throw new RuntimeException("Failed to sign data", e);
        }
    }
}

