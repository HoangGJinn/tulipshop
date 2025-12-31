package com.tulip.controller.api;

import com.tulip.entity.WishlistItem;
import com.tulip.service.WishlistService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/wishlist")
@RequiredArgsConstructor
public class WishlistApiController {

    private final WishlistService wishlistService;

    @GetMapping("/count")
    public Map<String, Object> count(@AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = wishlistService.count(userDetails.getUserId());
        return Map.of("count", count);
    }

    @GetMapping("/items")
    public List<Map<String, Object>> getItems(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<WishlistItem> items = wishlistService.getUserWishlist(userDetails.getUserId());
        return items.stream().map(i -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", i.getId());
            map.put("productId", i.getProduct().getId());
            map.put("name", i.getProduct().getName());
            map.put("thumbnail", i.getProduct().getThumbnail());
            map.put("basePrice", i.getProduct().getBasePrice());
            map.put("discountPrice", i.getProduct().getDiscountPrice());
            return map;
        }).toList();
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam Long productId) {
        boolean liked = wishlistService.toggle(userDetails.getUserId(), productId);
        long count = wishlistService.count(userDetails.getUserId());
        return Map.of("liked", liked, "count", count);
    }

    @GetMapping("/check")
    public Map<String, Object> check(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam Long productId) {
        boolean liked = wishlistService.isWishlisted(userDetails.getUserId(), productId);
        return Map.of("liked", liked);
    }
}
