package com.tulip.dto.response;

import com.tulip.entity.Order;
import com.tulip.entity.User;
import com.tulip.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private String address;
    private String role;
    private Boolean status;
    private String authProvider;
    private String createdAt;
    private String emailVerifiedAt;
    private Boolean isVerified;

    // Statistics
    private Integer totalOrders;
    private Integer pendingOrders;
    private Integer completedOrders;
    private Integer cancelledOrders;
    private Integer returnedOrders;
    private BigDecimal totalSpent;
    private String lastOrderDate;

    // Recent orders
    private List<OrderSummaryDTO> recentOrders;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummaryDTO {
        private Long id;
        private String orderCode;
        private String status;
        private String statusLabel;
        private String statusColor;
        private BigDecimal finalPrice;
        private Integer itemCount;
        private String createdAt;
        private String paymentMethod;
        private String paymentStatus;
    }

    public static CustomerDetailDTO fromEntity(User user, List<Order> orders) {
        if (user == null)
            return null;

        String fullName = "";
        String phone = "";
        String avatar = null;
        String address = "";

        if (user.getProfile() != null) {
            fullName = user.getProfile().getFullName() != null ? user.getProfile().getFullName() : "";
            phone = user.getProfile().getPhone() != null ? user.getProfile().getPhone() : "";
            avatar = user.getProfile().getAvatar();
            address = user.getProfile().getAddress() != null ? user.getProfile().getAddress() : "";
        }

        // Calculate statistics
        int totalOrders = orders != null ? orders.size() : 0;
        int pendingOrders = 0;
        int completedOrders = 0;
        int cancelledOrders = 0;
        int returnedOrders = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;
        LocalDateTime lastOrderDate = null;

        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) {
                    pendingOrders++;
                } else if (order.getStatus() == OrderStatus.DELIVERED) {
                    completedOrders++;
                    if (order.getFinalPrice() != null) {
                        totalSpent = totalSpent.add(order.getFinalPrice());
                    }
                } else if (order.getStatus() == OrderStatus.CANCELLED) {
                    cancelledOrders++;
                } else if (order.getStatus() == OrderStatus.RETURNED) {
                    returnedOrders++;
                } else if (order.getStatus() == OrderStatus.SHIPPING) {
                    pendingOrders++; // Count shipping as pending
                }

                if (order.getCreatedAt() != null) {
                    if (lastOrderDate == null || order.getCreatedAt().isAfter(lastOrderDate)) {
                        lastOrderDate = order.getCreatedAt();
                    }
                }
            }
        }

        // Get recent orders (last 10)
        List<OrderSummaryDTO> recentOrders = null;
        if (orders != null && !orders.isEmpty()) {
            recentOrders = orders.stream()
                    .limit(10)
                    .map(CustomerDetailDTO::toOrderSummary)
                    .collect(Collectors.toList());
        }

        return CustomerDetailDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(fullName)
                .phone(phone)
                .avatar(avatar)
                .address(address)
                .role(user.getRole() != null ? user.getRole().name() : "CUSTOMER")
                .status(Boolean.TRUE.equals(user.getStatus()))
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider() : "LOCAL")
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(FORMATTER) : null)
                .emailVerifiedAt(user.getEmailVerifiedAt() != null ? user.getEmailVerifiedAt().format(FORMATTER) : null)
                .isVerified(user.getEmailVerifiedAt() != null)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .returnedOrders(returnedOrders)
                .totalSpent(totalSpent)
                .lastOrderDate(lastOrderDate != null ? lastOrderDate.format(FORMATTER) : null)
                .recentOrders(recentOrders)
                .build();
    }

    private static OrderSummaryDTO toOrderSummary(Order order) {
        String statusLabel = "";
        String statusColor = "";

        switch (order.getStatus()) {
            case PENDING:
                statusLabel = "Chờ xác nhận";
                statusColor = "amber";
                break;
            case CONFIRMED:
                statusLabel = "Đã xác nhận";
                statusColor = "blue";
                break;
            case SHIPPING:
                statusLabel = "Đang giao";
                statusColor = "indigo";
                break;
            case DELIVERED:
                statusLabel = "Hoàn thành";
                statusColor = "green";
                break;
            case CANCELLED:
                statusLabel = "Đã hủy";
                statusColor = "red";
                break;
            case RETURNED:
                statusLabel = "Đã trả";
                statusColor = "orange";
                break;
            default:
                statusLabel = order.getStatus().getValue();
                statusColor = "gray";
        }

        int itemCount = 0;
        if (order.getOrderItems() != null) {
            itemCount = order.getOrderItems().stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();
        }

        return OrderSummaryDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().getValue())
                .statusLabel(statusLabel)
                .statusColor(statusColor)
                .finalPrice(order.getFinalPrice())
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : null)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .build();
    }
}
