package com.tulip.controller;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.dto.UserAddressDTO;
import com.tulip.dto.request.VnpayRequest;
import com.tulip.entity.Order;
import com.tulip.entity.PaymentMethod;
import com.tulip.repository.OrderRepository;
import com.tulip.security.JwtUtil;
import com.tulip.service.AddressService;
import com.tulip.service.CartService;
import com.tulip.service.OrderService;
import com.tulip.service.VnpayService;
import com.tulip.service.impl.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final VnpayService vnpayService;
    private final OrderRepository orderRepository;
    private final JwtUtil jwtUtil;

    // 1. Hiển thị trang Checkout
    @GetMapping("/checkout")
    public String viewCheckout(Model model, 
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpServletRequest request) {
        // Validate JWT token bằng method có sẵn trong JwtUtil
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUserId();

        // Lấy dữ liệu giỏ hàng
        List<CartItemDTO> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            return "redirect:/cart"; // Giỏ trống thì quay về trang giỏ
        }

        BigDecimal totalPrice = cartService.getTotalPrice(userId);
        BigDecimal shippingFee = new BigDecimal("30000"); // Phí ship cố định (demo)

        // Lấy danh sách địa chỉ của user để chọn
        List<UserAddressDTO> addresses = addressService.getUserAddresses(userId);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("finalPrice", totalPrice.add(shippingFee));
        model.addAttribute("addresses", addresses);

        // Object form để hứng dữ liệu
        model.addAttribute("orderRequest", new OrderCreationDTO());

        return "order/checkout";
    }

    // 2. Hiển thị trang kết quả đặt hàng thành công (cho COD)
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

    // 3. Xử lý nút "Đặt hàng"
    @PostMapping("/checkout/place-order")
    public String placeOrder(@ModelAttribute OrderCreationDTO orderRequest,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             HttpServletRequest httpRequest,
                             RedirectAttributes redirectAttributes) {
        // Validate JWT token bằng method có sẵn trong JwtUtil
        if (userDetails == null || !jwtUtil.validateJwtToken(httpRequest, userDetails)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        try {
            Order order = orderService.placeOrder(userDetails.getUserId(), orderRequest);
            
            // Kiểm tra phương thức thanh toán
            PaymentMethod paymentMethod = PaymentMethod.fromString(orderRequest.getPaymentMethod());
            
            if (paymentMethod == PaymentMethod.VNPAY) {
                // Tạo vnpTxnRef với format: TULIP-DDMMYYYY-Orderid-XXXX
                String vnpTxnRef = com.tulip.util.VnpayUtil.generateVnpTxnRef(order.getId());
                
                // Lưu vnpTxnRef vào Order trước khi tạo payment URL
                order.setVnpTxnRef(vnpTxnRef);
                orderRepository.save(order);
                
                // Tạo VNPAY payment request
                // Chuyển BigDecimal thành String số nguyên (VNPAY yêu cầu số tiền là số nguyên)
                BigDecimal finalPrice = order.getFinalPrice();
                if (finalPrice == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Không thể xác định tổng tiền đơn hàng");
                    return "redirect:/checkout";
                }
                // Làm tròn về số nguyên và chuyển thành String
                String amountString = finalPrice.setScale(0, RoundingMode.HALF_UP).toPlainString();
                VnpayRequest vnpayRequest = VnpayRequest.builder()
                        .amount(amountString)
                        .orderInfo("Thanh toan don hang " + vnpTxnRef)
                        .build();
                
                try {
                    // Tạo payment URL với vnpTxnRef đã được lưu vào Order
                    String paymentUrl = vnpayService.createPaymentWithTxnRef(vnpayRequest, vnpTxnRef, httpRequest);
                    return "redirect:" + paymentUrl;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo link thanh toán VNPAY: " + e.getMessage());
                    return "redirect:/checkout";
                }
            } else {
                // COD: Đặt hàng thành công -> Xóa giỏ hàng và chuyển sang trang thông báo
                // Tạo mã đơn hàng với format TULIP-DDMMYYYY-Orderid-XXXX cho COD
                String orderCode = com.tulip.util.VnpayUtil.generateVnpTxnRef(order.getId());
                order.setVnpTxnRef(orderCode);
                orderRepository.save(order);
                
                // Xóa giỏ hàng sau khi đặt hàng COD thành công (sử dụng service để có transaction)
                cartService.clearCart(userDetails.getUserId());
                
                // Redirect đến trang kết quả đặt hàng thành công
                return "redirect:/order-success?orderId=" + order.getId() + "&orderCode=" + orderCode;
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/checkout"; // Lỗi thì quay lại trang checkout
        }
    }
}