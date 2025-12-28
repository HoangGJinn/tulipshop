package com.tulip.service;

import com.tulip.dto.OrderCreationDTO;
import com.tulip.dto.response.OrderAdminDTO;
import com.tulip.entity.Order;
import com.tulip.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(Long userId, OrderCreationDTO request);
    
    List<Order> getUserOrders(Long userId);
    
    /**
     * Lấy danh sách đơn hàng của user với phân trang và lọc theo trạng thái
     * @param userId ID người dùng
     * @param status Trạng thái đơn hàng ("ALL" hoặc tên enum OrderStatus)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng đơn hàng mỗi trang
     * @return Page chứa danh sách đơn hàng
     */
    Page<Order> getOrdersByStatus(Long userId, String status, int page, int size);
    
    Optional<Order> getUserOrder(Long userId, Long orderId);
    
    /**
     * Mua lại đơn hàng đã hết hạn - thêm sản phẩm vào giỏ hàng
     * @param userId ID người dùng
     * @param orderId ID đơn hàng cũ (phải là EXPIRED)
     * @throws RuntimeException nếu đơn hàng không hợp lệ hoặc không đủ tồn kho
     */
    void reOrderToCart(Long userId, Long orderId);
    
    // ===== ADMIN METHODS =====

    List<OrderAdminDTO> getAllOrders();

    List<OrderAdminDTO> getOrdersByStatus(OrderStatus status);

    List<OrderAdminDTO> getPendingOrders();

    List<OrderAdminDTO> getOrdersByUser(Long userId);

    List<OrderAdminDTO> getOrdersByDate(LocalDate date);

    void confirmOrder(Long orderId);

    void startShipping(Long orderId);
}