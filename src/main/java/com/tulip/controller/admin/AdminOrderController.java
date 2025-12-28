package com.tulip.controller.admin;

import com.tulip.dto.response.OrderAdminDTO;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    
    private final OrderService orderService;
    
    @GetMapping
    public ResponseEntity<List<OrderAdminDTO>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "PENDING") String defaultStatus) {
        
        // Nếu không có filter nào thì lấy đơn hàng PENDING mặc định
        if ((keyword == null || keyword.isEmpty()) && 
            (dateFrom == null || dateFrom.isEmpty()) && 
            (dateTo == null || dateTo.isEmpty())) {
            
            // Nếu defaultStatus = "ALL" thì lấy tất cả, ngược lại lấy theo status
            if ("ALL".equalsIgnoreCase(defaultStatus)) {
                List<OrderAdminDTO> orders = orderService.getAllOrders();
                return ResponseEntity.ok(orders);
            } else {
                try {
                    OrderStatus status = OrderStatus.valueOf(defaultStatus.toUpperCase());
                    List<OrderAdminDTO> orders = orderService.getOrdersByStatus(status);
                    return ResponseEntity.ok(orders);
                } catch (IllegalArgumentException e) {
                    // Nếu status không hợp lệ, trả về PENDING
                    List<OrderAdminDTO> orders = orderService.getPendingOrders();
                    return ResponseEntity.ok(orders);
                }
            }
        }
        
        // Nếu có filter thì xử lý filter
        List<OrderAdminDTO> orders = orderService.getAllOrders();
        
        // TODO: Implement filtering logic if needed
        // For now, return all orders sorted by date
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Lấy đơn hàng theo trạng thái
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderAdminDTO>> getOrdersByStatus(@PathVariable String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<OrderAdminDTO> orders = orderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Lấy đơn hàng chờ xác nhận
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OrderAdminDTO>> getPendingOrders() {
        List<OrderAdminDTO> orders = orderService.getPendingOrders();
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Lấy đơn hàng theo user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderAdminDTO>> getOrdersByUser(@PathVariable Long userId) {
        List<OrderAdminDTO> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Lấy đơn hàng theo ngày
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<OrderAdminDTO>> getOrdersByDate(@PathVariable String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<OrderAdminDTO> orders = orderService.getOrdersByDate(localDate);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Xác nhận đơn hàng (PENDING -> CONFIRMED)
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) {
        try {
            orderService.confirmOrder(orderId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xác nhận đơn hàng thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Bắt đầu giao hàng (gọi shipping service)
     */
    @PostMapping("/{orderId}/start-shipping")
    public ResponseEntity<?> startShipping(@PathVariable Long orderId) {
        try {
            orderService.startShipping(orderId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Đã gửi đơn hàng cho đơn vị vận chuyển");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Lấy thống kê đơn hàng theo trạng thái
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", (long) orderService.getPendingOrders().size());
        stats.put("CONFIRMED", (long) orderService.getOrdersByStatus(OrderStatus.CONFIRMED).size());
        stats.put("SHIPPING", (long) orderService.getOrdersByStatus(OrderStatus.SHIPPING).size());
        stats.put("DELIVERED", (long) orderService.getOrdersByStatus(OrderStatus.DELIVERED).size());
        return ResponseEntity.ok(stats);
    }
}
