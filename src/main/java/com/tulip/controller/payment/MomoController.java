package com.tulip.controller.payment;

import com.tulip.dto.request.MomoRequest;
import com.tulip.service.MomoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/momo")
@RequiredArgsConstructor
public class MomoController {

    private final MomoService momoService;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody MomoRequest paymentRequest) {
        String requestType = paymentRequest.getRequestType() != null ? paymentRequest.getRequestType() : "payWithMethod";
        String payUrl = momoService.createPaymentRequest(
                paymentRequest.getAmount(),
                paymentRequest.getOrderId(),
                paymentRequest.getOrderInfo(),
                requestType
        );
        return ResponseEntity.ok(Map.of("payUrl", payUrl));
    }

    @GetMapping("/order-status/{orderId}")
    public ResponseEntity<String> checkPaymentStatus(@PathVariable String orderId) {
        String status = momoService.checkPaymentStatus(orderId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleMomoCallback(@RequestBody Map<String, Object> callbackData) {
        momoService.processCallback(callbackData);
        return ResponseEntity.noContent().build();
    }
}
