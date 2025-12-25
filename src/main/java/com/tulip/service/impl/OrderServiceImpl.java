package com.tulip.service.impl;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.entity.*;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.entity.enums.PaymentStatus;
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
                .status(OrderStatus.PENDING)
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
    
    @Override
    @Transactional
    public void reOrderToCart(Long userId, Long orderId) {
        // 1. Lấy đơn hàng cũ và kiểm tra quyền sở hữu
        Order oldOrder = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        
        if (!oldOrder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập đơn hàng này");
        }
        
        // 2. Chỉ cho phép mua lại đơn hàng đã CANCELLED do hết hạn thanh toán
        if (oldOrder.getStatus() != OrderStatus.CANCELLED || 
            oldOrder.getPaymentStatus() != PaymentStatus.EXPIRED) {
            throw new RuntimeException("Chỉ có thể mua lại đơn hàng đã hết hạn thanh toán");
        }
        
        // 3. Kiểm tra tồn kho và thêm vào giỏ hàng
        if (oldOrder.getOrderItems() == null || oldOrder.getOrderItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm nào");
        }
        
        List<String> unavailableItems = new ArrayList<>();
        
        for (OrderItem item : oldOrder.getOrderItems()) {
            if (item.getStock() == null) {
                unavailableItems.add(item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm không xác định");
                continue;
            }
            
            ProductStock stock = productStockRepository.findById(item.getStock().getId())
                    .orElse(null);
            
            if (stock == null) {
                unavailableItems.add(item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm không xác định");
                continue;
            }
            
            int requestedQuantity = item.getQuantity();
            int availableQuantity = stock.getQuantity();
            
            if (availableQuantity <= 0) {
                unavailableItems.add((item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm") + 
                        " (Size: " + (item.getSize() != null ? item.getSize().getCode() : "N/A") + ") - Hết hàng");
                continue;
            }
            
            // Thêm vào giỏ với số lượng tối đa có thể (nếu yêu cầu nhiều hơn có sẵn)
            int quantityToAdd = Math.min(requestedQuantity, availableQuantity);
            try {
                cartService.addToCart(userId, stock.getId(), quantityToAdd);
                
                // Thông báo nếu số lượng ít hơn yêu cầu
                if (quantityToAdd < requestedQuantity) {
                    unavailableItems.add((item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm") + 
                            " (Size: " + (item.getSize() != null ? item.getSize().getCode() : "N/A") + 
                            ") - Chỉ còn " + availableQuantity + " sản phẩm (đã thêm " + quantityToAdd + " vào giỏ)");
                }
            } catch (Exception e) {
                unavailableItems.add((item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm") + 
                        " - " + e.getMessage());
            }
        }
        
        // 4. Nếu có sản phẩm không khả dụng, throw exception với thông tin chi tiết
        if (!unavailableItems.isEmpty()) {
            String message = "Một số sản phẩm không thể thêm vào giỏ hàng:\n" + 
                    String.join("\n", unavailableItems);
            throw new RuntimeException(message);
        }
    }
}