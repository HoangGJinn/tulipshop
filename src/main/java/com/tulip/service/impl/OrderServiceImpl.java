package com.tulip.service.impl;

import com.tulip.dto.CartItemDTO;
import com.tulip.dto.OrderCreationDTO;
import com.tulip.dto.response.OrderAdminDTO;
import com.tulip.dto.response.ShippingRateResponse;
import com.tulip.entity.*;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.entity.enums.PaymentStatus;
import com.tulip.entity.product.ProductStock;
import com.tulip.repository.*;
import com.tulip.service.CartService;
import com.tulip.service.OrderService;
import com.tulip.service.integration.TulipShippingClient;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final TulipShippingClient shippingClient;
    private final ShippingOrderRepository shippingOrderRepository;

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

        String shippingAddress = address.getFullAddress();
        BigDecimal totalPrice = cartService.getTotalPrice(userId);

        // --- Logic tính phí ship từ API ---
        BigDecimal shippingFee;
        String deliveryType = (request.getDeliveryType() != null && !request.getDeliveryType().isEmpty())
                ? request.getDeliveryType()
                : "STANDARD";

        try {
            ShippingRateResponse rateResponse = shippingClient.getShippingFee(shippingAddress, deliveryType);
            shippingFee = rateResponse.getShippingFee();
        } catch (Exception e) {
            // Fallback nếu shipping service lỗi: dùng phí mặc định 30k
            System.err.println("Lỗi gọi Shipping Service: " + e.getMessage());
            shippingFee = new BigDecimal("30000");
        }

        BigDecimal finalPrice = totalPrice.add(shippingFee);

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .shippingPrice(shippingFee)
                .finalPrice(finalPrice)
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.fromString(request.getPaymentMethod()))
                .paymentStatus(PaymentStatus.PENDING)
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
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
    public Page<Order> getOrdersByStatus(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Order> orderPage;
        if ("ALL".equalsIgnoreCase(status)) {
            orderPage = orderRepository.findByUserIdPaginated(userId, pageable);
        } else {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByUserIdAndStatusPaginated(userId, orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                // Nếu status không hợp lệ, trả về tất cả
                orderPage = orderRepository.findByUserIdPaginated(userId, pageable);
            }
        }
        
        // Initialize lazy-loaded relationships
        for (Order order : orderPage.getContent()) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getVariant() != null) {
                        Hibernate.initialize(item.getVariant().getImages());
                    }
                }
            }
        }
        
        return orderPage;
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
        Order oldOrder = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!oldOrder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập đơn hàng này");
        }

        if (oldOrder.getStatus() != OrderStatus.CANCELLED ||
                oldOrder.getPaymentStatus() != PaymentStatus.EXPIRED) {
            throw new RuntimeException("Chỉ có thể mua lại đơn hàng đã hết hạn thanh toán");
        }

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

            int quantityToAdd = Math.min(requestedQuantity, availableQuantity);
            try {
                cartService.addToCart(userId, stock.getId(), quantityToAdd);

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

        if (!unavailableItems.isEmpty()) {
            String message = "Một số sản phẩm không thể thêm vào giỏ hàng:\n" +
                    String.join("\n", unavailableItems);
            throw new RuntimeException(message);
        }
    }
    
    // ===== ADMIN METHODS =====
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderAdminDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        // Sort theo ngày mới nhất
        orders.sort((o1, o2) -> {
            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
            if (o1.getCreatedAt() == null) return 1;
            if (o2.getCreatedAt() == null) return -1;
            return o2.getCreatedAt().compareTo(o1.getCreatedAt()); // DESC
        });
        return orders.stream().map(this::convertToDTO).toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderAdminDTO> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == status)
            .sorted((o1, o2) -> {
                if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                if (o1.getCreatedAt() == null) return 1;
                if (o2.getCreatedAt() == null) return -1;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt()); // DESC
            })
            .toList();
        return orders.stream().map(this::convertToDTO).toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderAdminDTO> getPendingOrders() {
        return getOrdersByStatus(OrderStatus.PENDING);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderAdminDTO> getOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getUser() != null && o.getUser().getId().equals(userId))
            .toList();
        return orders.stream().map(this::convertToDTO).toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderAdminDTO> getOrdersByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCreatedAt() != null && 
                        !o.getCreatedAt().isBefore(startOfDay) && 
                        !o.getCreatedAt().isAfter(endOfDay))
            .toList();
        
        return orders.stream().map(this::convertToDTO).toList();
    }
    
    @Override
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));
        
        // Chỉ có thể xác nhận đơn hàng PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xác nhận đơn hàng ở trạng thái PENDING");
        }
        
        // Kiểm tra payment cho MOMO và VNPAY
        if (order.getPaymentMethod() == PaymentMethod.MOMO || order.getPaymentMethod() == PaymentMethod.VNPAY) {
            // Kiểm tra payment status
            if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Đơn hàng chưa thanh toán thành công. Vui lòng kiểm tra trạng thái thanh toán.");
            }
            
            // Kiểm tra payment expiry
            if (order.getPaymentExpireAt() != null && LocalDateTime.now().isAfter(order.getPaymentExpireAt())) {
                throw new RuntimeException("Đơn hàng đã hết hạn thanh toán. Không thể xác nhận.");
            }
        }
        
        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        
        // Cập nhật trạng thái shipping order
        ShippingOrder shippingOrder = shippingOrderRepository.findByOrder_Id(orderId)
                .orElse(null);
        
        if (shippingOrder != null) {
            shippingOrder.setStatus(OrderStatus.CONFIRMED);
            shippingOrderRepository.save(shippingOrder);
        }
        
        // KHÔNG gọi API shipping ở đây nữa!
        // Chỉ xác nhận đơn hàng, chưa bắt đầu giao
    }
    
    @Override
    @Transactional
    public void startShipping(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));
        
        // Chỉ có thể bắt đầu giao hàng khi đơn đã CONFIRMED
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể bắt đầu giao hàng cho đơn hàng đã xác nhận");
        }
        
        // Cập nhật trạng thái đơn hàng sang SHIPPING
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);
        
        // Cập nhật trạng thái shipping order
        ShippingOrder shippingOrder = shippingOrderRepository.findByOrder_Id(orderId)
                .orElse(null);
        
        if (shippingOrder != null) {
            shippingOrder.setStatus(OrderStatus.SHIPPING);
            shippingOrderRepository.save(shippingOrder);
        }
        
        // GỌI API SHIPPING SERVICE ĐỂ BẮT ĐẦU GIAO HÀNG
        try {
            shippingClient.startDelivery(order.getOrderCode());
        } catch (Exception e) {
            // Rollback trạng thái nếu gọi API thất bại
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            if (shippingOrder != null) {
                shippingOrder.setStatus(OrderStatus.CONFIRMED);
                shippingOrderRepository.save(shippingOrder);
            }
            throw new RuntimeException("Lỗi khi gọi API vận chuyển: " + e.getMessage());
        }
    }
    
    // Helper method to convert Order entity to DTO
    private OrderAdminDTO convertToDTO(Order order) {
        List<OrderAdminDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
        
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                // Lấy ảnh đầu tiên của variant
                String productImage = null;
                if (item.getVariant() != null && item.getVariant().getImages() != null && !item.getVariant().getImages().isEmpty()) {
                    productImage = item.getVariant().getImages().get(0).getImageUrl();
                }
                
                OrderAdminDTO.OrderItemDTO itemDTO = OrderAdminDTO.OrderItemDTO.builder()
                    .id(item.getId())
                    .productName(item.getProduct() != null ? item.getProduct().getName() : "N/A")
                    .productImage(productImage)
                    .variantColorName(item.getVariant() != null ? item.getVariant().getColorName() : "N/A")
                    .sizeCode(item.getSize() != null ? item.getSize().getCode() : "N/A")
                    .sku(item.getSku())
                    .quantity(item.getQuantity())
                    .priceAtPurchase(item.getPriceAtPurchase())
                    .build();
                itemDTOs.add(itemDTO);
            }
        }
        
        // Lấy thông tin người đặt hàng
        String userName = null;
        String userPhone = null;
        String userEmail = null;
        
        if (order.getUser() != null) {
            userEmail = order.getUser().getEmail();
            Hibernate.initialize(order.getUser().getProfile());
            if (order.getUser().getProfile() != null) {
                userName = order.getUser().getProfile().getFullName();
                userPhone = order.getUser().getProfile().getPhone();
            }
        }
        
        return OrderAdminDTO.builder()
            .id(order.getId())
            .orderCode(order.getOrderCode())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .userEmail(userEmail != null ? userEmail : "N/A")
            .userName(userName != null ? userName : "N/A")
            .userPhone(userPhone != null ? userPhone : "N/A")
            .recipientName(order.getRecipientName())
            .recipientPhone(order.getRecipientPhone())
            .shippingAddress(order.getShippingAddress())
            .totalPrice(order.getTotalPrice())
            .shippingPrice(order.getShippingPrice())
            .finalPrice(order.getFinalPrice())
            .status(order.getStatus())
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .paymentExpireAt(order.getPaymentExpireAt())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .orderItems(itemDTOs)
            .build();
    }
}