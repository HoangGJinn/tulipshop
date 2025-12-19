package com.tulip.service.impl;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.entity.*;
import com.tulip.entity.product.ProductStock;
import com.tulip.entity.product.ProductVariant;
import com.tulip.repository.*;
import com.tulip.service.CartService;
import com.tulip.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        List<CartItemDTO> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống, không thể đặt hàng");
        }

        UserAddress address = userAddressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ"));

        String shippingAddress = String.format("%s, %s, %s, %s - SĐT: %s (Người nhận: %s)",
                address.getAddressLine(),
                address.getVillage(),
                address.getDistrict(),
                address.getProvince(),
                address.getRecipientPhone(),
                address.getRecipientName());

        BigDecimal totalPrice = cartService.getTotalPrice(userId);
        BigDecimal shippingFee = new BigDecimal("30000");
        BigDecimal finalPrice = totalPrice.add(shippingFee);

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .shippingPrice(shippingFee)
                .finalPrice(finalPrice)
                .status(Order.OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.fromString(request.getPaymentMethod()))
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(shippingAddress)
                .orderItems(new ArrayList<>())
                .build();

        for (CartItemDTO itemDTO : cartItems) {
            CartItem cartItemEntity = cartItemRepository.findById(itemDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Item không tồn tại trong giỏ"));

            ProductStock realStock = cartItemEntity.getStock();

            if (realStock.getQuantity() < itemDTO.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + itemDTO.getProductName() + " không đủ số lượng!");
            }

            realStock.setQuantity(realStock.getQuantity() - itemDTO.getQuantity());
            productStockRepository.save(realStock);

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

        return orderRepository.save(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Order order : orders) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getVariant() != null) {
                        Hibernate.initialize(item.getVariant().getImages());
                    }
                }
            }
        }
        return orders;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Order> getUserOrder(Long userId, Long orderId) {
        Optional<Order> orderOpt = orderRepository.findByIdWithDetails(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getUser().getId().equals(userId)) {
                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        if (item.getVariant() != null) {
                            Hibernate.initialize(item.getVariant().getImages());
                        }
                    }
                }
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }
}