package com.tulip.service.impl;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.entity.*;
import com.tulip.entity.product.ProductStock;
import com.tulip.repository.*;
import com.tulip.service.CartService;
import com.tulip.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final ProductStockRepository productStockRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public Order placeOrder(Long userId, OrderCreationDTO request) {
        // 1. Lấy thông tin User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // 2. Lấy giỏ hàng hiện tại
        List<CartItemDTO> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống, không thể đặt hàng");
        }

        // 3. Lấy địa chỉ giao hàng
        UserAddress address = userAddressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ"));

        // --- SỬA LẠI ĐOẠN NÀY ĐỂ KHỚP VỚI FILE USERADDRESS CỦA BẠN ---
        // Sử dụng getAddressLine, getVillage, getProvince thay vì getStreet, getWard...
        String shippingAddress = String.format("%s, %s, %s, %s - SĐT: %s (Người nhận: %s)",
                address.getAddressLine(),
                address.getVillage(),
                address.getDistrict(),
                address.getProvince(),
                address.getRecipientPhone(),
                address.getRecipientName());
        // --------------------------------------------------------------

        // 4. Tính toán tổng tiền
        BigDecimal totalPrice = cartService.getTotalPrice(userId);
        BigDecimal shippingFee = new BigDecimal("30000"); // Phí ship cố định
        BigDecimal finalPrice = totalPrice.add(shippingFee);

        // 5. Khởi tạo Order
        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .shippingPrice(shippingFee)
                .finalPrice(finalPrice)
                .status(Order.OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .shippingAddress(shippingAddress)
                .orderItems(new ArrayList<>())
                .build();

        // 6. Xử lý OrderItem & Trừ kho
        for (CartItemDTO itemDTO : cartItems) {
            // Lấy CartItem entity để truy xuất Stock
            CartItem cartItemEntity = cartItemRepository.findById(itemDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Item không tồn tại trong giỏ"));

            ProductStock realStock = cartItemEntity.getStock();

            // Check tồn kho
            if (realStock.getQuantity() < itemDTO.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + itemDTO.getProductName() + " không đủ số lượng!");
            }

            // Trừ kho
            realStock.setQuantity(realStock.getQuantity() - itemDTO.getQuantity());
            productStockRepository.save(realStock);

            // Tạo OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(realStock.getVariant().getProduct())
                    .variant(realStock.getVariant())
                    .size(realStock.getSize())
                    .stock(realStock)
                    .sku(realStock.getSku())
                    .quantity(itemDTO.getQuantity())
                    .priceAtPurchase(itemDTO.getPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        // 7. Lưu Order
        Order savedOrder = orderRepository.save(order);

        // 8. Xóa sạch giỏ hàng
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteAllByCartId(cart.getId());
        }

        return savedOrder;
    }
}