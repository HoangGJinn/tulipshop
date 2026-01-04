package com.tulip.dto;

import com.tulip.entity.Voucher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String type; // PERCENT, AMOUNT, or FREESHIP
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer quantity;
    private Integer usedCount;
    private Integer remaining;
    private String startAt; // Formatted as string for JSON
    private String expireAt; // Formatted as string for JSON
    private Boolean status;
    private Boolean isValid;
    private Boolean isPublic;

    // New fields for checkout display
    private BigDecimal calculatedDiscount; // Số tiền giảm thực tế cho đơn hàng hiện tại
    private Boolean canApply; // Có thể áp dụng hay không
    private String disabledReason; // Lý do không thể áp dụng

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Factory method to create VoucherDTO from Voucher entity
     */
    public static VoucherDTO fromEntity(Voucher voucher) {
        if (voucher == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        int total = voucher.getQuantity() != null ? voucher.getQuantity() : 0;
        int remaining = Math.max(0, total - used);

        // Check if voucher is currently valid
        boolean isValid = Boolean.TRUE.equals(voucher.getStatus())
                && (voucher.getStartAt() == null || !now.isBefore(voucher.getStartAt()))
                && (voucher.getExpireAt() == null || !now.isAfter(voucher.getExpireAt()))
                && remaining > 0;

        return VoucherDTO.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .type(voucher.getType() != null ? voucher.getType().name() : null)
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .startAt(voucher.getStartAt() != null ? voucher.getStartAt().format(FORMATTER) : null)
                .expireAt(voucher.getExpireAt() != null ? voucher.getExpireAt().format(FORMATTER) : null)
                .quantity(total)
                .usedCount(used)
                .remaining(remaining)
                .status(voucher.getStatus())
                .isValid(isValid)
                .isPublic(voucher.getIsPublic())
                .build();
    }
}
