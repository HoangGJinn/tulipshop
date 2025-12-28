package com.tulip.controller.payment;

import com.tulip.dto.request.VnpayRequest;
import com.tulip.dto.response.VnpayResponse;
import com.tulip.entity.Order;
import com.tulip.repository.OrderRepository;
import com.tulip.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
public class VnpayController {

    @Autowired
    private VnpayService vnpayService;
    
    @Autowired
    private OrderRepository orderRepository;

    // 1. Endpoint tạo URL thanh toán (API endpoint - dùng @ResponseBody)
    @PostMapping("/v1/api/vnpay/create-payment")
    @ResponseBody
    public ResponseEntity<VnpayResponse> createPayment(@RequestBody VnpayRequest vnpayRequest, HttpServletRequest request) {
        try {
            String paymentUrl = vnpayService.createPayment(vnpayRequest, request);

            VnpayResponse response = VnpayResponse.builder()
                    .code("200")
                    .message("Successfully created payment link")
                    .paymentUrl(paymentUrl)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VnpayResponse.builder()
                            .code("500")
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }

    // 2. Endpoint nhận kết quả trả về từ VNPAY (vnp_ReturnUrl)
    @GetMapping("/v1/api/vnpay/payment-callback")
    public String paymentCallback(HttpServletRequest request, Model model) {
        // Xử lý callback và cập nhật Order
        Long orderId = vnpayService.handlePaymentCallback(request);
        
        if (orderId != null) {
            // Lấy Order để lấy orderCode
            Order order = orderRepository.findById(orderId).orElse(null);
            
            // Lấy response code để hiển thị thông báo
            String responseCode = request.getParameter("vnp_ResponseCode");
            boolean isSuccess = "00".equals(responseCode);
            
            model.addAttribute("success", isSuccess);
            model.addAttribute("orderId", orderId);
            // Hiển thị orderCode (mã đơn hàng) cho khách hàng
            if (order != null && order.getOrderCode() != null) {
                model.addAttribute("orderCode", order.getOrderCode());
            }
            model.addAttribute("message", isSuccess ? "Thanh toán thành công!" : "Thanh toán thất bại!");
            
            return "payment/payment-result";
        } else {
            // Lỗi: không tìm thấy đơn hàng hoặc signature không hợp lệ
            model.addAttribute("success", false);
            model.addAttribute("message", "Lỗi xử lý thanh toán. Vui lòng liên hệ hỗ trợ.");
            return "payment/payment-result";
        }
    }
}