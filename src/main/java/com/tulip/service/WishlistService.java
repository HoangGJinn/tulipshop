package com.tulip.service;

import com.tulip.entity.WishlistItem;

import java.util.List;

public interface WishlistService {
    boolean isWishlisted(Long userId, Long productId);

    /**
     * Toggle wishlist status.
     * @return true if now wishlisted, false if removed
     */
    boolean toggle(Long userId, Long productId);

    void remove(Long userId, Long productId);

    List<WishlistItem> getUserWishlist(Long userId);

    long count(Long userId);
}
