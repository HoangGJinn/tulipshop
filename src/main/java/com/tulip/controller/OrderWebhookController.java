package com.tulip.controller;

import com.tulip.entity.Order;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.entity.enums.PaymentStatus;
import com.tulip.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class OrderWebhookController {

    private final OrderRepository orderRepository;

    @Data
    public static class ShippingStatusUpdateDTO {
        private String orderCode;
        private String status;
        private String message;
    }

    @PostMapping("/shipping-update")
    @Transactional // 2. Đảm bảo tính toàn vẹn dữ liệu (nếu có lỗi sẽ rollback)
    public ResponseEntity<?> updateOrderStatus(@RequestBody ShippingStatusUpdateDTO request) {
        log.info(">>> Webhook received for Order: {}, Status: {}", request.getOrderCode(), request.getStatus());

        try {
            // 3. Validation đầu vào cơ bản
            if (request.getOrderCode() == null || request.getStatus() == null) {
                log.warn("Webhook request missing data");
                return ResponseEntity.badRequest().body("OrderCode and Status are required");
            }

            Order order = orderRepository.findByOrderCode(request.getOrderCode());
            if (order == null) {
                log.warn("Order not found with code: {}", request.getOrderCode());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
            }

            // 4. Xử lý logic (Dùng Switch-Case cho rõ ràng và dễ mở rộng)
            String shippingStatus = request.getStatus().toUpperCase();

            switch (shippingStatus) {
                case "DELIVERED":
                    handleDeliveredStatus(order);
                    break;

                case "RETURNED":
                    handleReturnedStatus(order);
                    break;

                case "DELIVERING":
                    // Có thể thêm logic cập nhật trạng thái đang giao nếu cần
                    order.setStatus(OrderStatus.SHIPPING);
                    break;

                default:
                    log.info("Status '{}' not handled, ignoring.", shippingStatus);
            }

            orderRepository.save(order);
            log.info("Order {} updated successfully to {}", order.getOrderCode(), order.getStatus());

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            // 5. Catch lỗi hệ thống (DB connection, NullPointer...)
            log.error("Error processing webhook for order: {}", request.getOrderCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }


    private void handleDeliveredStatus(Order order) {
        order.setStatus(OrderStatus.DELIVERED);
        // Nếu là COD và giao thành công -> Đã thu tiền
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.SUCCESS); // Hoặc PAID tùy enum của bạn
            log.info("COD Order {} - Payment status updated to PAID", order.getOrderCode());
        }
    }

    private void handleReturnedStatus(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order {} marked as RETURNED/CANCELLED", order.getOrderCode());
        // Có thể thêm logic hoàn tiền (Refund) nếu cần
    }
}