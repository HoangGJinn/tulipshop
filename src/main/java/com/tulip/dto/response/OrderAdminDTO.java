package com.tulip.dto.response;

import com.tulip.entity.enums.OrderStatus;
import com.tulip.entity.enums.PaymentMethod;
import com.tulip.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAdminDTO {
    private Long id;
    private String orderCode;
    
    // Thông tin người đặt hàng (khách hàng)
    private Long userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    
    // Thông tin người nhận
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    
    // Thông tin đơn hàng
    private BigDecimal totalPrice;
    private BigDecimal shippingPrice;
    private BigDecimal finalPrice;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentExpireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> orderItems;
    
    /**
     * Kiểm tra xem đơn hàng có thể được xác nhận hay không
     * Đơn hàng COD luôn có thể xác nhận
     * Đơn hàng MOMO/VNPAY phải thanh toán thành công và chưa hết hạn
     */
    public boolean canBeConfirmed() {
        // Đơn hàng phải ở trạng thái PENDING
        if (this.status != OrderStatus.PENDING) {
            return false;
        }
        
        // Nếu COD thì luôn có thể xác nhận
        if (this.paymentMethod == PaymentMethod.COD) {
            return true;
        }
        
        // Nếu thanh toán online (MOMO/VNPAY)
        if (this.paymentMethod == PaymentMethod.MOMO || this.paymentMethod == PaymentMethod.VNPAY) {
            // Phải thanh toán thành công
            if (this.paymentStatus != PaymentStatus.SUCCESS) {
                return false;
            }
            
            // Kiểm tra thời hạn thanh toán nếu có
            if (this.paymentExpireAt != null) {
                return !LocalDateTime.now().isAfter(this.paymentExpireAt);
            }
            
            return true;
        }
        
        return false;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Long id;
        private String productName;
        private String productImage;
        private String variantColorName;
        private String sizeCode;
        private String sku;
        private Integer quantity;
        private BigDecimal priceAtPurchase;
    }
}
