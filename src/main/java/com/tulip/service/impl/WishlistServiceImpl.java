package com.tulip.service.impl;

import com.tulip.entity.User;
import com.tulip.entity.WishlistItem;
import com.tulip.entity.product.Product;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.UserRepository;
import com.tulip.repository.WishlistItemRepository;
import com.tulip.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long productId) {
        return wishlistItemRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public boolean toggle(Long userId, Long productId) {
        if (wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
            return false;
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();

        wishlistItemRepository.save(item);
        return true;
    }

    @Override
    public void remove(Long userId, Long productId) {
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItem> getUserWishlist(Long userId) {
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Long userId) {
        return wishlistItemRepository.countByUserId(userId);
    }
}
