package com.tulip.controller;

import com.tulip.dto.CartItemDTO;
import com.tulip.service.CartService;
import com.tulip.service.impl.CustomUserDetails;
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
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để mua hàng");
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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. API Cập nhật số lượng (AJAX)
    @PostMapping("/v1/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateQuantity(@RequestParam Long itemId,
                                            @RequestParam int quantity,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();

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
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();

        cartService.removeFromCart(userDetails.getUserId(), itemId);
        return ResponseEntity.ok("Đã xóa");
    }
}