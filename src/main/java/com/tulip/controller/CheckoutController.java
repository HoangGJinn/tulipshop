package com.tulip.controller;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.dto.UserAddressDTO;
import com.tulip.dto.request.VnpayRequest;
import com.tulip.entity.Order;
import com.tulip.entity.PaymentMethod;
import com.tulip.repository.OrderRepository;
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

    // 1. Hiển thị trang Checkout
    @GetMapping("/checkout")
    public String viewCheckout(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";

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

    // 2. Xử lý nút "Đặt hàng"
    @PostMapping("/checkout/place-order")
    public String placeOrder(@ModelAttribute OrderCreationDTO orderRequest,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             HttpServletRequest httpRequest,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        try {
            Order order = orderService.placeOrder(userDetails.getUserId(), orderRequest);
            
            // Kiểm tra phương thức thanh toán
            PaymentMethod paymentMethod = PaymentMethod.fromString(orderRequest.getPaymentMethod());
            
            if (paymentMethod == PaymentMethod.VNPAY) {
                // Tạo vnpTxnRef random
                String vnpTxnRef = com.tulip.util.VnpayUtil.getRandomNumber(8);
                
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
                        .orderInfo("Thanh toan don hang #" + order.getId())
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
                // COD: Đặt hàng thành công -> Chuyển sang trang thông báo
                redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn: #" + order.getId());
                return "redirect:/"; // Tạm thời về trang chủ (bạn có thể tạo trang /order-success)
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/checkout"; // Lỗi thì quay lại trang checkout
        }
    }
}