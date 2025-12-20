package com.tulip.controller.payment;

import com.tulip.entity.Order;
import com.tulip.entity.enums.PaymentStatus;
import com.tulip.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class MomoRedirectController {
    
    private final OrderRepository orderRepository;
    
    @GetMapping("/v1/api/momo/payment-callback")
    @Transactional
    public String paymentCallback(@RequestParam(required = false) String orderId,
                                  @RequestParam(required = false) Integer resultCode,
                                  @RequestParam(required = false) String message,
                                  @RequestParam(required = false) String transId,
                                  Model model) {
        boolean isSuccess = resultCode != null && resultCode == 0;
        Order order = null;
        
        if (orderId != null) {
            try {
                order = orderRepository.findByVnpTxnRef(orderId);
                
                if (order == null) {
                    Long extractedOrderId = com.tulip.util.VnpayUtil.extractOrderIdFromVnpTxnRef(orderId);
                    if (extractedOrderId != null) {
                        order = orderRepository.findById(extractedOrderId).orElse(null);
                    }
                }
                
                if (order == null) {
                    Long orderIdLong = Long.parseLong(orderId);
                    order = orderRepository.findById(orderIdLong).orElse(null);
                }
            } catch (NumberFormatException e) {
                log.warn("Cannot parse orderId: {}", orderId);
            }
        }
        
        if (order != null) {
            if (isSuccess) {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                if (transId != null) {
                    order.setTransactionId(transId);
                }
                log.info("Payment successful for order: {}", order.getId());
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                log.warn("Payment failed for order: {}, resultCode: {}", order.getId(), resultCode);
            }
            orderRepository.save(order);
            
            model.addAttribute("orderId", order.getId());
            if (order.getVnpTxnRef() != null) {
                model.addAttribute("orderCode", order.getVnpTxnRef());
            }
        } else {
            log.error("Order not found for orderId: {}", orderId);
            if (orderId != null) {
                model.addAttribute("orderId", orderId);
            }
        }
        
        model.addAttribute("success", isSuccess);
        model.addAttribute("message", isSuccess ? "Thanh toán thành công!" : 
                          (message != null ? message : "Thanh toán thất bại!"));
        
        return "payment/payment-result";
    }
}

