package com.tulip.controller;

import com.tulip.dto.CartItemDTO;
import com.tulip.security.JwtUtil;
import com.tulip.service.CartService;
import com.tulip.service.impl.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    // 1. Xem trang giỏ hàng
    @GetMapping("/cart")
    public String viewCart(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login"; // Bắt buộc đăng nhập
        }

        List<CartItemDTO> cartItems = cartService.getCartItems(userDetails.getUserId());
        BigDecimal totalPrice = cartService.getTotalPrice(userDetails.getUserId());

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);

        return "order/cart"; // Trả về file templates/order/cart.html
    }

    // 2. API Thêm vào giỏ (AJAX)
    @PostMapping("/v1/api/cart/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestParam Long stockId,
                                       @RequestParam int quantity,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       HttpServletRequest request) {
        // Kiểm tra đăng nhập trước
        if (userDetails == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        // Validate JWT token bằng method có sẵn trong JwtUtil
        if (!jwtUtil.validateJwtToken(request, userDetails)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại");
            return ResponseEntity.status(401).body(errorResponse);
        }

        try {
            cartService.addToCart(userDetails.getUserId(), stockId, quantity);

            // Trả về số lượng mới để update icon giỏ hàng
            int newCount = cartService.countItems(userDetails.getUserId());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã thêm vào giỏ hàng");
            response.put("totalItems", newCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 3. API Cập nhật số lượng (AJAX)
    @PostMapping("/v1/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateQuantity(@RequestParam Long itemId,
                                            @RequestParam int quantity,
                                            @AuthenticationPrincipal CustomUserDetails userDetails,
                                            HttpServletRequest request) {
        // Validate JWT token bằng method có sẵn trong JwtUtil
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc đã hết hạn");
        }

        try {
            cartService.updateQuantity(userDetails.getUserId(), itemId, quantity);
            BigDecimal newTotal = cartService.getTotalPrice(userDetails.getUserId());
            return ResponseEntity.ok(newTotal);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. API Xóa item (AJAX)
    @DeleteMapping("/v1/api/cart/remove/{itemId}")
    @ResponseBody
    public ResponseEntity<?> removeItem(@PathVariable Long itemId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpServletRequest request) {
        // Validate JWT token bằng method có sẵn trong JwtUtil
        if (userDetails == null || !jwtUtil.validateJwtToken(request, userDetails)) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc đã hết hạn");
        }

        cartService.removeFromCart(userDetails.getUserId(), itemId);
        return ResponseEntity.ok("Đã xóa");
    }
}