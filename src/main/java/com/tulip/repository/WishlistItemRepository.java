package com.tulip.repository;

import com.tulip.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    long countByUserId(Long userId);
    
    List<WishlistItem> findByProductId(Long productId);
}
