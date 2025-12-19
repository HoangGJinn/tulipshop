package com.tulip.controller;

import com.tulip.entity.Order;
import com.tulip.repository.OrderRepository;
import com.tulip.security.JwtUtil;
import com.tulip.service.OrderService;
import com.tulip.service.impl.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final JwtUtil jwtUtil;
    
    @GetMapping("/orders")
    public String viewOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                            HttpServletRequest request,
                            Model model) {
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return "redirect:/login";
        }
        
        List<Order> orders = orderService.getUserOrders(userDetails.getUserId());
        model.addAttribute("orders", orders);
        model.addAttribute("user", userDetails);
        
        return "order/orders";
    }
    
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
}

