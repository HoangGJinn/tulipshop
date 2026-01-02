package com.tulip.controller;

import com.tulip.entity.Order;
import com.tulip.security.JwtUtil;
import com.tulip.service.OrderService;
import com.tulip.service.impl.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    // --- 1. HIỂN THỊ DANH SÁCH ĐƠN HÀNG (Có Phân trang & Lọc) ---
    @GetMapping("/orders")
    public String viewOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(name = "status", defaultValue = "ALL") String status, // Mặc định là ALL
                             @RequestParam(name = "page", defaultValue = "0") int page,          // Mặc định trang 0
                             @RequestParam(name = "size", defaultValue = "5") int size,          // 5 đơn mỗi trang
                             HttpServletRequest request,
                             Model model) {

        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return "redirect:/login";
        }

        // Gọi Service lấy dữ liệu phân trang
        Page<Order> orderPage = orderService.getOrdersByStatus(userDetails.getUserId(), status, page, size);

        model.addAttribute("orders", orderPage.getContent()); // List đơn hàng của trang hiện tại
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("currentStatus", status);
        model.addAttribute("user", userDetails);

        return "order/orders";
    }

    // --- 2. CHI TIẾT ĐƠN HÀNG (Giữ nguyên) ---
    @GetMapping("/orders/{orderId}")
    public String viewOrderDetail(@PathVariable Long orderId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpServletRequest request,
                                  Model model) {
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderService.getUserOrder(userDetails.getUserId(), orderId);

        if (orderOpt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này");
            return "order/order-detail";
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("user", userDetails);

        return "order/order-detail";
    }

    // --- 3. MUA LẠI ĐƠN HÀNG (Giữ nguyên) ---
    @PostMapping("/orders/{orderId}/re-order")
    public String reOrder(@PathVariable Long orderId,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/login";
        }

        try {
            orderService.reOrderToCart(userDetails.getUserId(), orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng.");
            return "redirect:/checkout";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/orders/" + orderId;
        }
    }
}