package com.tulip.controller;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.dto.UserAddressDTO;
import com.tulip.dto.request.ShippingOrderRequest;
import com.tulip.dto.request.VnpayRequest;
import com.tulip.dto.response.ShippingRateResponse;
import com.tulip.entity.Order;
import com.tulip.entity.ShippingOrder;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.repository.OrderRepository;
import com.tulip.repository.ShippingOrderRepository;
import com.tulip.security.JwtUtil;
import com.tulip.service.AddressService;
import com.tulip.service.CartService;
import com.tulip.service.OrderService;
import com.tulip.service.MomoService;
import com.tulip.service.VnpayService;
import com.tulip.service.integration.TulipShippingClient;
import com.tulip.service.impl.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final VnpayService vnpayService;
    private final MomoService momoService;
    private final OrderRepository orderRepository;
    private final ShippingOrderRepository shippingOrderRepository;
    private final JwtUtil jwtUtil;
    private final TulipShippingClient shippingClient;
    private final com.tulip.service.VoucherService voucherService;

    // 1. Hiển thị trang Checkout
    @GetMapping("/checkout")
    public String viewCheckout(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUserId();
        List<CartItemDTO> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty())
            return "redirect:/cart";

        List<UserAddressDTO> addresses = addressService.getUserAddresses(userId);
        BigDecimal totalPrice = cartService.getTotalPrice(userId, items);

        // Mặc định Standard
        BigDecimal shippingFee = new BigDecimal("30000");
        String estimatedTime = "3-5 ngày";

        UserAddressDTO defaultAddress = addresses.stream()
                .filter(UserAddressDTO::getIsDefault).findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.get(0));

        if (defaultAddress != null) {
            try {
                // Sử dụng getFullAddress() từ DTO cho gọn
                String fullAddr = defaultAddress.getFullAddress();
                if (fullAddr == null || fullAddr.isEmpty()) {
                    // Fallback nếu DTO chưa có getFullAddress
                    fullAddr = defaultAddress.getAddressLine() + ", " + defaultAddress.getProvince();
                }

                ShippingRateResponse rate = shippingClient.getShippingFee(fullAddr, "STANDARD");
                if (rate != null) {
                    shippingFee = rate.getShippingFee();
                    estimatedTime = rate.getEstimatedTime();
                }
            } catch (Exception e) {
                // Log lỗi nhưng không crash trang checkout
                System.err.println("Lỗi kết nối shipping service: " + e.getMessage());
                // Giữ giá mặc định, không cần làm gì
            }
        }

        // Lấy danh sách voucher có thể áp dụng
        List<com.tulip.entity.Voucher> applicableVouchers = voucherService.getApplicableVouchers(totalPrice);
        if (applicableVouchers == null)
            applicableVouchers = new java.util.ArrayList<>();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("estimatedTime", estimatedTime);
        model.addAttribute("finalPrice", totalPrice.add(shippingFee));
        if (addresses == null)
            addresses = new java.util.ArrayList<>();
        model.addAttribute("addresses", addresses);

        OrderCreationDTO orderRequest = new OrderCreationDTO();
        orderRequest.setCheckoutItems(items); // Set danh sách item đã chọn để submit lại khi đặt hàng
        model.addAttribute("orderRequest", orderRequest);

        model.addAttribute("applicableVouchers", applicableVouchers);

        return "order/checkout";
    }

    // 2. API AJAX cập nhật phí ship (Sửa đổi: Thêm tham số deliveryType)
    @GetMapping("/checkout/api/calculate-fee")
    @ResponseBody
    public ResponseEntity<?> calculateShippingFee(@RequestParam Long addressId,
            @RequestParam(defaultValue = "STANDARD") String deliveryType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UserAddressDTO addr = addressService.getUserAddressById(addressId);
            if (addr == null)
                return ResponseEntity.badRequest().body("Địa chỉ không tồn tại");

            // Gọi API với loại vận chuyển khách chọn
            String fullAddr = addr.getFullAddress();
            ShippingRateResponse response = shippingClient.getShippingFee(fullAddr, deliveryType);

            // Tính lại tổng tiền cuối cùng
            BigDecimal cartTotal = cartService.getTotalPrice(userDetails.getUserId());
            BigDecimal newFinalPrice = cartTotal.add(response.getShippingFee());

            Map<String, Object> result = new HashMap<>();
            result.put("shippingFee", response.getShippingFee());
            result.put("estimatedTime", response.getEstimatedTime());
            result.put("finalPrice", newFinalPrice);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // API Áp dụng voucher
    @GetMapping("/checkout/api/apply-voucher")
    @ResponseBody
    public ResponseEntity<?> applyVoucher(@RequestParam String code,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null)
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");

            BigDecimal cartTotal = cartService.getTotalPrice(userDetails.getUserId());

            if (!voucherService.isValid(code, cartTotal)) {
                return ResponseEntity.badRequest().body("Voucher không hợp lệ hoặc không đủ điều kiện áp dụng");
            }

            BigDecimal discount = voucherService.calculateDiscount(code, cartTotal);

            Map<String, Object> result = new HashMap<>();
            result.put("discountAmount", discount);
            result.put("newTotal", cartTotal.subtract(discount));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi kiểm tra voucher: " + e.getMessage());
        }
    }

    // 3. Xử lý đặt hàng
    @PostMapping("/checkout/place-order")
    public String placeOrder(@ModelAttribute OrderCreationDTO orderRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null)
            return "redirect:/login";

        try {
            // Service đã tự tính toán phí ship dựa trên orderRequest.getDeliveryType()
            Order order = orderService.placeOrder(userDetails.getUserId(), orderRequest);

            // --- Xử lý thanh toán ---
            PaymentMethod paymentMethod = PaymentMethod.fromString(orderRequest.getPaymentMethod());

            // cartService.clearCart(userId); // Đã xử lý bên trong placeOrder để hỗ trợ
            // partial checkout
            String orderCode = com.tulip.util.VnpayUtil.generateOrderCode(order.getId());
            order.setOrderCode(orderCode);
            orderRepository.save(order);

            // [QUAN TRỌNG] Gửi sang Shipping Service với đúng loại vận chuyển
            this.sendToShippingService(order, orderRequest.getDeliveryType(), paymentMethod);

            if (paymentMethod == PaymentMethod.VNPAY) {
                String amountString = order.getFinalPrice().setScale(0, RoundingMode.HALF_UP).toPlainString();
                VnpayRequest vnpayRequest = VnpayRequest.builder()
                        .amount(amountString).orderInfo("Thanh toan don hang " + orderCode).build();
                try {
                    String paymentUrl = vnpayService.createPaymentWithOrderCode(vnpayRequest, orderCode, httpRequest);
                    order.setPaymentUrl(paymentUrl);
                    order.setPaymentExpireAt(LocalDateTime.now().plusMinutes(15));
                    orderRepository.save(order);
                    return "redirect:" + paymentUrl;
                } catch (UnsupportedEncodingException e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi VNPAY");
                    return "redirect:/checkout";
                }
            } else if (paymentMethod == PaymentMethod.MOMO) {
                String amountString = order.getFinalPrice().setScale(0, RoundingMode.HALF_UP).toPlainString();
                try {
                    String paymentUrl = momoService.createPaymentRequest(amountString, orderCode,
                            "Thanh toan don hang " + orderCode, "payWithMethod");
                    order.setPaymentUrl(paymentUrl);
                    order.setPaymentExpireAt(LocalDateTime.now().plusMinutes(15));
                    orderRepository.save(order);
                    return "redirect:" + paymentUrl;
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi MoMo");
                    return "redirect:/checkout";
                }
            }

            return "redirect:/order-success?orderId=" + order.getId() + "&orderCode=" + orderCode;

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    // Hàm phụ trợ gửi vận đơn (Sửa đổi: Nhận thêm deliveryType)
    private void sendToShippingService(Order order, String deliveryType, PaymentMethod paymentMethod) {
        try {
            BigDecimal codAmount = paymentMethod == PaymentMethod.COD ? order.getFinalPrice() : BigDecimal.ZERO;
            // 1. Tạo ShippingOrder entity để lưu vào database local
            ShippingOrder shippingOrder = ShippingOrder.builder()
                    .order(order)
                    .deliveryType(deliveryType != null ? deliveryType : "STANDARD")
                    .shippingFee(order.getShippingPrice())
                    .status(OrderStatus.PENDING)
                    .carrier("Tulip Shipping")
                    .codAmount(codAmount)
                    .build();

            // Lưu vào database local
            shippingOrderRepository.save(shippingOrder);

            // 2. Gọi API shipping service bên ngoài (nếu có)
            ShippingOrderRequest shipRequest = ShippingOrderRequest.builder()
                    .orderCode(order.getOrderCode())
                    .receiverName(order.getRecipientName())
                    .receiverPhone(order.getRecipientPhone())
                    .receiverAddress(order.getShippingAddress())
                    .deliveryType(deliveryType != null ? deliveryType : "STANDARD")
                    .codAmount(codAmount)
                    .build();
            shippingClient.createShippingOrder(shipRequest);
        } catch (Exception e) {
            System.err.println("Không thể tạo vận đơn shipping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/order-success")
    public String orderSuccess(@RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String orderCode,
            Model model) {
        model.addAttribute("success", true);
        model.addAttribute("orderId", orderId);
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("message", "Đặt hàng thành công!");
        return "payment/payment-result";
    }
}