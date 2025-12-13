package com.tulip.controller;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.dto.UserAddressDTO;
import com.tulip.entity.Order;
import com.tulip.service.AddressService;
import com.tulip.service.CartService;
import com.tulip.service.OrderService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;

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

        return "checkout";
    }

    // 2. Xử lý nút "Đặt hàng"
    @PostMapping("/checkout/place-order")
    public String placeOrder(@ModelAttribute OrderCreationDTO orderRequest,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        try {
            Order order = orderService.placeOrder(userDetails.getUserId(), orderRequest);

            // Đặt hàng thành công -> Chuyển sang trang thông báo (hoặc trang lịch sử)
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn: #" + order.getId());
            return "redirect:/"; // Tạm thời về trang chủ (bạn có thể tạo trang /order-success)

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/checkout"; // Lỗi thì quay lại trang checkout
        }
    }
}