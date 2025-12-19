package com.tulip.service.impl;

import com.tulip.dto.CartItemDTO;
import com.tulip.entity.Cart;
import com.tulip.entity.CartItem;
import com.tulip.entity.User;
import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductStock;
import com.tulip.entity.product.ProductVariantImage;
import com.tulip.repository.*;
import com.tulip.service.CartService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductStockRepository productStockRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void addToCart(Long userId, Long stockId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Lấy hoặc tạo giỏ hàng
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });

        // 2. Lấy thông tin Stock (Sản phẩm + Size + Màu cụ thể)
        ProductStock stock = productStockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại hoặc đã hết hàng"));

        if (stock.getQuantity() < quantity) {
            throw new RuntimeException("Số lượng tồn kho không đủ (Còn lại: " + stock.getQuantity() + ")");
        }

        // 3. Kiểm tra xem sản phẩm này đã có trong giỏ chưa
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndStockId(cart.getId(), stockId);

        if (existingItem.isPresent()) {
            // Nếu có rồi -> Cộng dồn số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Nếu chưa -> Tạo mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .stock(stock)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null) return new ArrayList<>();

        return cart.getCartItems().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item không tồn tại"));

        // Bảo mật: Check xem item này có đúng là của user đang đăng nhập không
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền truy cập");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item); // Xóa nếu số lượng = 0
        } else {
            // Check tồn kho
            if (quantity > item.getStock().getQuantity()) {
                throw new RuntimeException("Vượt quá số lượng tồn kho");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId).orElse(null);
        if (item != null && item.getCart().getUser().getId().equals(userId)) {
            cartItemRepository.delete(item);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countItems(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null) return 0;
        return cart.getCartItems().stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalPrice(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null) return BigDecimal.ZERO;

        return cart.getCartItems().stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null && cart.getId() != null) {
            // Xóa tất cả cart items
            cartItemRepository.deleteAllByCartId(cart.getId());
            // Flush để đảm bảo thay đổi được commit ngay lập tức
            entityManager.flush();
            entityManager.clear(); // Clear persistence context để tránh stale data
        }
    }

    // Helper: Convert Entity -> DTO
    private CartItemDTO convertToDTO(CartItem item) {
        ProductStock stock = item.getStock();
        Product product = stock.getVariant().getProduct();

        // Lấy ảnh: Ưu tiên ảnh của Variant, nếu không có lấy ảnh Product
        String img = product.getThumbnail();
        try {
            // Force initialize images collection trong cùng transaction
            Hibernate.initialize(stock.getVariant().getImages());
            List<ProductVariantImage> varImgs = stock.getVariant().getImages();
            if (varImgs != null && !varImgs.isEmpty()) {
                img = varImgs.get(0).getImageUrl();
            }
        } catch (Exception e) {
            // Nếu có lỗi lazy loading, sử dụng thumbnail của product
            // (đã được set ở trên)
        }

        // Lấy giá bán (đã giảm giá nếu có)
        BigDecimal currentPrice = product.getDiscountPrice() != null
                ? product.getDiscountPrice()
                : product.getBasePrice();

        return CartItemDTO.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(img)
                .colorName(stock.getVariant().getColorName())
                .sizeCode(stock.getSize().getCode())
                .price(currentPrice)
                .quantity(item.getQuantity())
                .subTotal(item.getSubTotal())
                .maxStock(stock.getQuantity())
                .build();
    }
}