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
import com.tulip.service.EmailService;
import com.tulip.service.NotificationService;
import com.tulip.service.OrderService;
import com.tulip.service.integration.TulipShippingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final ProductStockRepository productStockRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final EmailService emailService;
    private final TulipShippingClient shippingClient;
    private final ShippingOrderRepository shippingOrderRepository;
    private final com.tulip.service.VoucherService voucherService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Order placeOrder(Long userId, OrderCreationDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Get all cart items (no filtering by selected items)
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

        // --- Logic Voucher ---
        BigDecimal finalPrice = totalPrice.add(shippingFee);
        Voucher voucher = null;
        
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            String code = request.getVoucherCode().trim();
            // Validate voucher
            if (voucherService.isValid(code, totalPrice)) {
                BigDecimal discount = voucherService.calculateDiscount(code, totalPrice);
                finalPrice = finalPrice.subtract(discount);
                if (finalPrice.compareTo(BigDecimal.ZERO) < 0)
                    finalPrice = BigDecimal.ZERO;

                voucher = voucherService.getVoucherByCode(code).orElse(null);

                // Update voucher usage
                if (voucher != null) {
                    voucher.setUsedCount(voucher.getUsedCount() + 1);
                    voucherService.saveVoucher(voucher);
                }
            }
        }

        Order order = Order.builder()
                .user(user)
                .voucher(voucher) // Set Voucher
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

            // if (realStock.getQuantity() < itemDTO.getQuantity()) {
            // throw new RuntimeException("Sản phẩm " + itemDTO.getProductName() + " không
            // đủ số lượng!");
            // }
            //
            // realStock.setQuantity(realStock.getQuantity() - itemDTO.getQuantity());
            // productStockRepository.save(realStock);

            // Snapshot thông tin sản phẩm tại thời điểm đặt hàng
            String snapProductName = realStock.getVariant().getProduct().getName();
            BigDecimal snapPrice = itemDTO.getPrice();
            String snapThumbnailUrl = realStock.getVariant().getProduct().getThumbnail();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(realStock.getVariant().getProduct())
                    .variant(realStock.getVariant())
                    .size(realStock.getSize())
                    .stock(realStock)
                    .sku(realStock.getSku())
                    .quantity(itemDTO.getQuantity())
                    .priceAtPurchase(itemDTO.getPrice())
                    // Snapshot fields - Đóng băng thông tin
                    .snapProductName(snapProductName)
                    .snapPrice(snapPrice)
                    .snapThumbnailUrl(snapThumbnailUrl)
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // SAU KHI ĐẶT HÀNG THÀNH CÔNG -> XÓA CÁC ITEMS ĐÃ MUA KHỎI GIỎ
        // Since we're processing all cart items, clear the entire cart
        cartService.clearCart(userId);

        log.info("📦 Order #{} saved successfully. Preparing to send confirmation email...", savedOrder.getId());

        // Eager load relationships before async email sending to avoid
        // LazyInitializationException
        Hibernate.initialize(savedOrder.getUser());
        if (savedOrder.getUser().getProfile() != null) {
            Hibernate.initialize(savedOrder.getUser().getProfile());
        }
        Hibernate.initialize(savedOrder.getOrderItems());
        for (OrderItem item : savedOrder.getOrderItems()) {
            if (item.getProduct() != null) {
                Hibernate.initialize(item.getProduct());
            }
            if (item.getVariant() != null) {
                Hibernate.initialize(item.getVariant());
                Hibernate.initialize(item.getVariant().getImages());
            }
            if (item.getSize() != null) {
                Hibernate.initialize(item.getSize());
            }
        }

        log.info("📧 Calling emailService.sendOrderUpdateEmail for order #{}", savedOrder.getId());

        // Send order confirmation email asynchronously
        try {
            emailService.sendOrderUpdateEmail(savedOrder);
            log.info("✅ Email service called successfully for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error calling email service for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }

        // Gửi thông báo real-time qua WebSocket
        try {
            com.tulip.dto.NotificationRequest notificationRequest = com.tulip.dto.NotificationRequest.builder()
                .title("Đặt hàng thành công")
                .content("Đơn hàng #" + savedOrder.getId() + " đã được đặt thành công. Tổng giá trị: " + 
                        String.format("%,.0f", savedOrder.getFinalPrice()) + " VNĐ")
                .link("/orders/" + savedOrder.getId())
                .type(com.tulip.entity.Notification.NotificationType.ORDER)
                .build();
            
            notificationService.sendNotification(user.getEmail(), notificationRequest);
            log.info("🔔 Notification sent successfully for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending notification for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }

        return savedOrder;
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
                unavailableItems
                        .add(item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm không xác định");
                continue;
            }

            ProductStock stock = productStockRepository.findById(item.getStock().getId())
                    .orElse(null);

            if (stock == null) {
                unavailableItems
                        .add(item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm không xác định");
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
            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null)
                return 0;
            if (o1.getCreatedAt() == null)
                return 1;
            if (o2.getCreatedAt() == null)
                return -1;
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
                    if (o1.getCreatedAt() == null && o2.getCreatedAt() == null)
                        return 0;
                    if (o1.getCreatedAt() == null)
                        return 1;
                    if (o2.getCreatedAt() == null)
                        return -1;
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
                throw new RuntimeException(
                        "Đơn hàng chưa thanh toán thành công. Vui lòng kiểm tra trạng thái thanh toán.");
            }

            // Kiểm tra payment expiry
            if (order.getPaymentExpireAt() != null && LocalDateTime.now().isAfter(order.getPaymentExpireAt())) {
                throw new RuntimeException("Đơn hàng đã hết hạn thanh toán. Không thể xác nhận.");
            }
        }

        // Logic: Nếu là COD thì bây giờ mới trừ kho.
        // Còn Momo/VNPAY thì đã trừ lúc Callback (confirmOrderPayment) rồi nên bỏ qua.
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            for (OrderItem item : order.getOrderItems()) {
                ProductStock stock = item.getStock();

                // Tính tồn kho mới
                int newQuantity = stock.getQuantity() - item.getQuantity();

                // Kiểm tra âm kho (Safety check)
                if (newQuantity < 0) {
                    throw new RuntimeException("Không đủ tồn kho cho sản phẩm: " + stock.getSku());
                }

                // Cập nhật và lưu xuống DB
                stock.setQuantity(newQuantity);
                productStockRepository.save(stock);
            }
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        // Cập nhật trạng thái shipping order
        ShippingOrder shippingOrder = shippingOrderRepository.findByOrder_Id(orderId)
                .orElse(null);

        if (shippingOrder != null) {
            shippingOrder.setStatus(OrderStatus.CONFIRMED);
            shippingOrderRepository.save(shippingOrder);
        }

        // Eager load relationships before async email sending
        Hibernate.initialize(savedOrder.getUser());
        if (savedOrder.getUser().getProfile() != null) {
            Hibernate.initialize(savedOrder.getUser().getProfile());
        }
        Hibernate.initialize(savedOrder.getOrderItems());
        for (OrderItem item : savedOrder.getOrderItems()) {
            if (item.getProduct() != null) {
                Hibernate.initialize(item.getProduct());
            }
            if (item.getVariant() != null) {
                Hibernate.initialize(item.getVariant());
                Hibernate.initialize(item.getVariant().getImages());
            }
            if (item.getSize() != null) {
                Hibernate.initialize(item.getSize());
            }
        }

        // Send CONFIRMED email
        log.info("📧 Sending CONFIRMED email for order #{}", savedOrder.getId());
        try {
            emailService.sendOrderUpdateEmail(savedOrder);
            log.info("✅ CONFIRMED email service called successfully for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error calling email service for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
        
        // Gửi thông báo real-time qua WebSocket
        try {
            com.tulip.dto.NotificationRequest notificationRequest = com.tulip.dto.NotificationRequest.builder()
                .title("Đơn hàng đã được xác nhận")
                .content("Đơn hàng #" + savedOrder.getId() + " đã được xác nhận và đang được chuẩn bị.")
                .link("/orders/" + savedOrder.getId())
                .type(com.tulip.entity.Notification.NotificationType.ORDER)
                .build();
            
            notificationService.sendNotification(savedOrder.getUser().getEmail(), notificationRequest);
            log.info("🔔 Notification sent successfully for order #{} confirmation", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending notification for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
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
        Order savedOrder = orderRepository.save(order);

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

        // Eager load relationships before async email sending
        Hibernate.initialize(savedOrder.getUser());
        if (savedOrder.getUser().getProfile() != null) {
            Hibernate.initialize(savedOrder.getUser().getProfile());
        }
        Hibernate.initialize(savedOrder.getOrderItems());
        for (OrderItem item : savedOrder.getOrderItems()) {
            if (item.getProduct() != null) {
                Hibernate.initialize(item.getProduct());
            }
            if (item.getVariant() != null) {
                Hibernate.initialize(item.getVariant());
                Hibernate.initialize(item.getVariant().getImages());
            }
            if (item.getSize() != null) {
                Hibernate.initialize(item.getSize());
            }
        }

        // Send SHIPPING email
        log.info("📧 Sending SHIPPING email for order #{}", savedOrder.getId());
        try {
            emailService.sendOrderUpdateEmail(savedOrder);
            log.info("✅ SHIPPING email service called successfully for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error calling email service for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
        
        // Gửi thông báo real-time qua WebSocket
        try {
            com.tulip.dto.NotificationRequest notificationRequest = com.tulip.dto.NotificationRequest.builder()
                .title("Đơn hàng đang được giao")
                .content("Đơn hàng #" + savedOrder.getId() + " đang trên đường giao đến bạn.")
                .link("/orders/" + savedOrder.getId())
                .type(com.tulip.entity.Notification.NotificationType.ORDER)
                .build();
            
            notificationService.sendNotification(savedOrder.getUser().getEmail(), notificationRequest);
            log.info("🔔 Notification sent successfully for order #{} shipping", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending notification for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));

        // Chỉ có thể hoàn thành đơn hàng khi đang SHIPPING
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new RuntimeException("Chỉ có thể hoàn thành đơn hàng đang vận chuyển");
        }

        // Cập nhật trạng thái đơn hàng sang DELIVERED
        order.setStatus(OrderStatus.DELIVERED);
        Order savedOrder = orderRepository.save(order);

        // Cập nhật trạng thái shipping order
        ShippingOrder shippingOrder = shippingOrderRepository.findByOrder_Id(orderId)
                .orElse(null);

        if (shippingOrder != null) {
            shippingOrder.setStatus(OrderStatus.DELIVERED);
            shippingOrderRepository.save(shippingOrder);
        }

        // Eager load relationships before async email sending
        Hibernate.initialize(savedOrder.getUser());
        if (savedOrder.getUser().getProfile() != null) {
            Hibernate.initialize(savedOrder.getUser().getProfile());
        }
        Hibernate.initialize(savedOrder.getOrderItems());
        for (OrderItem item : savedOrder.getOrderItems()) {
            if (item.getProduct() != null) {
                Hibernate.initialize(item.getProduct());
            }
            if (item.getVariant() != null) {
                Hibernate.initialize(item.getVariant());
                Hibernate.initialize(item.getVariant().getImages());
            }
            if (item.getSize() != null) {
                Hibernate.initialize(item.getSize());
            }
        }

        // Send DELIVERED email
        log.info("📧 Sending DELIVERED email for order #{}", savedOrder.getId());
        try {
            emailService.sendOrderUpdateEmail(savedOrder);
            log.info("✅ DELIVERED email service called successfully for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error calling email service for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
        
        // Gửi thông báo real-time qua WebSocket
        try {
            com.tulip.dto.NotificationRequest notificationRequest = com.tulip.dto.NotificationRequest.builder()
                .title("Đơn hàng đã được giao")
                .content("Đơn hàng #" + savedOrder.getId() + " đã được giao thành công. Cảm ơn bạn đã mua hàng!")
                .link("/orders/" + savedOrder.getId())
                .type(com.tulip.entity.Notification.NotificationType.ORDER)
                .build();
            
            notificationService.sendNotification(savedOrder.getUser().getEmail(), notificationRequest);
            log.info("🔔 Notification sent successfully for order #{} delivery", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending notification for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
        
        // Gửi thông báo nhắc đánh giá sản phẩm
        try {
            com.tulip.dto.NotificationRequest ratingReminderRequest = com.tulip.dto.NotificationRequest.builder()
                .title("Đánh giá sản phẩm")
                .content("Bạn đã nhận được hàng? Hãy chia sẻ trải nghiệm của bạn để giúp người mua khác!")
                .link("/orders/" + savedOrder.getId())
                .type(com.tulip.entity.Notification.NotificationType.SYSTEM)
                .build();
            
            notificationService.sendNotification(savedOrder.getUser().getEmail(), ratingReminderRequest);
            log.info("⭐ Rating reminder notification sent for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending rating reminder for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
        
        // Gửi email nhắc đánh giá
        try {
            emailService.sendRatingReminderEmail(savedOrder);
            log.info("📧 Rating reminder email sent for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending rating reminder email for order #{}: {}", savedOrder.getId(), e.getMessage(), e);
        }
    }

    // Helper method to convert Order entity to DTO
    private OrderAdminDTO convertToDTO(Order order) {
        List<OrderAdminDTO.OrderItemDTO> itemDTOs = new ArrayList<>();

        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                // Lấy ảnh đầu tiên của variant
                String productImage = null;
                if (item.getVariant() != null && item.getVariant().getImages() != null
                        && !item.getVariant().getImages().isEmpty()) {
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

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId, String reason) {
        // 1. Tìm kiếm đơn hàng
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 2. Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
        }

        // 3. Kiểm tra trạng thái - chỉ cho phép hủy khi PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái Chờ xử lý");
        }

        // 4. Cập nhật trạng thái và lý do hủy
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);

        // 5. Hoàn tác voucher nếu có
        if (order.getVoucher() != null) {
            Voucher voucher = order.getVoucher();
            if (voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherService.saveVoucher(voucher);
                log.info("✅ Voucher {} usage count decreased for cancelled order #{}", 
                        voucher.getCode(), orderId);
            }
        }

        // 6. Cập nhật payment status nếu là thanh toán online
        if (order.getPaymentMethod() == PaymentMethod.MOMO || 
            order.getPaymentMethod() == PaymentMethod.VNPAY) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }

        // 7. Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);
        log.info("📦 Order #{} has been cancelled by user #{}", orderId, userId);

        // 8. Eager load relationships trước khi gửi thông báo
        Hibernate.initialize(savedOrder.getUser());
        if (savedOrder.getUser().getProfile() != null) {
            Hibernate.initialize(savedOrder.getUser().getProfile());
        }

        // 9. Gửi thông báo cho người dùng
        try {
            com.tulip.dto.NotificationRequest notificationRequest = com.tulip.dto.NotificationRequest.builder()
                .title("Đơn hàng đã được hủy")
                .content("Đơn hàng #" + savedOrder.getId() + " đã được hủy thành công. Lý do: " + reason)
                .link("/orders/" + savedOrder.getId())
                .type(com.tulip.entity.Notification.NotificationType.ORDER)
                .build();
            
            notificationService.sendNotification(savedOrder.getUser().getEmail(), notificationRequest);
            log.info("🔔 Cancellation notification sent successfully for order #{}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Error sending cancellation notification for order #{}: {}", 
                    savedOrder.getId(), e.getMessage(), e);
        }

        // Lưu ý: Reserved stock sẽ tự động được giải phóng vì InventoryService 
        // tính toán reservedStock dựa trên các đơn hàng chưa hoàn thành/hủy
    }

    @Override
    @Transactional
    public void confirmOrderPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Chỉ trừ kho nếu đơn hàng chuyển từ trạng thái giữ chỗ (PENDING) sang đã thanh
        // toán
        if (order.getStatus() == OrderStatus.PENDING) {
            for (OrderItem item : order.getOrderItems()) {
                ProductStock stock = item.getStock();
                // Bây giờ mới thực sự trừ kho vật lý
                int newQuantity = stock.getQuantity() - item.getQuantity();
                if (newQuantity < 0) {
                    throw new RuntimeException("Kho không đủ để hoàn tất đơn hàng này (Lỗi bất thường)");
                }

                // Gọi service update để đảm bảo có Lock và lưu Lịch sử (History)
                // Lưu ý: Cần inject InventoryService vào OrderServiceImpl để gọi hàm này
                // Hoặc update trực tiếp tại đây và tự tạo history:
                stock.setQuantity(newQuantity);
                productStockRepository.save(stock);

                // TODO: Lưu StockHistory tại đây nếu muốn lưu vết là "Đơn hàng thành công"
            }

            // Cập nhật trạng thái đơn hàng
            // order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            Order savedOrder = orderRepository.save(order);

            // Eager load relationships before async email sending
            Hibernate.initialize(savedOrder.getUser());
            if (savedOrder.getUser().getProfile() != null) {
                Hibernate.initialize(savedOrder.getUser().getProfile());
            }
            Hibernate.initialize(savedOrder.getOrderItems());
            for (OrderItem item : savedOrder.getOrderItems()) {
                if (item.getProduct() != null) {
                    Hibernate.initialize(item.getProduct());
                }
                if (item.getVariant() != null) {
                    Hibernate.initialize(item.getVariant());
                    Hibernate.initialize(item.getVariant().getImages());
                }
                if (item.getSize() != null) {
                    Hibernate.initialize(item.getSize());
                }
            }

            // Send order confirmation email for online payment
            emailService.sendOrderUpdateEmail(savedOrder);
        }
    }

    @Override
    @Transactional
    public void handlePaymentFailure(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Nếu đơn hàng đang chờ thanh toán mà bị lỗi, cập nhật trạng thái
        if (order.getStatus() == OrderStatus.PENDING) {
            // có thể set là CANCELLED hoặc tạo thêm enum PAYMENT_FAILED tùy logic
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED); // Cần đảm bảo enum PaymentStatus có giá trị FAILED

            // Lưu ý: Code cũ chưa trừ kho ở bước PENDING nên không cần cộng lại kho ở đây.
            // Nếu logic thay đổi (đã trừ kho từ lúc đặt), thì phải cộng lại kho ở đây.

            orderRepository.save(order);
        }
    }
}