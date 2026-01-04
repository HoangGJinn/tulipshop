package com.tulip.dto.response;

import com.tulip.entity.UserVoucher;
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
public class UserVoucherDTO {
    private Long id;
    private Long voucherId;
    private String code;
    private String name;
    private String description;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private String expireAt;
    private Boolean isUsed;
    private String usedAt;
    private String receivedAt;
    private String source;
    private String sourceLabel;
    private Boolean isExpired;
    private String displayValue;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static UserVoucherDTO fromEntity(UserVoucher uv) {
        if (uv == null || uv.getVoucher() == null)
            return null;

        Voucher v = uv.getVoucher();
        LocalDateTime now = LocalDateTime.now();

        // Xác định thời hạn: ưu tiên expireAt của UserVoucher, nếu không có thì lấy của
        // Voucher
        LocalDateTime effectiveExpireAt = uv.getExpireAt() != null ? uv.getExpireAt() : v.getExpireAt();
        boolean isExpired = effectiveExpireAt != null && now.isAfter(effectiveExpireAt);

        // Format giá trị hiển thị
        String displayValue = "";
        if (v.getType() == Voucher.DiscountType.PERCENT) {
            displayValue = v.getDiscountValue().intValue() + "%";
        } else if (v.getType() == Voucher.DiscountType.AMOUNT) {
            displayValue = formatCurrency(v.getDiscountValue());
        } else if (v.getType() == Voucher.DiscountType.FREESHIP) {
            displayValue = "Miễn phí vận chuyển";
        }

        // Source label
        String sourceLabel = "";
        if ("RATING".equals(uv.getSource())) {
            sourceLabel = "Quà tặng đánh giá";
        } else if ("EVENT".equals(uv.getSource())) {
            sourceLabel = "Sự kiện";
        } else if ("GIFT".equals(uv.getSource())) {
            sourceLabel = "Quà tặng";
        } else if ("PROMO".equals(uv.getSource())) {
            sourceLabel = "Khuyến mãi";
        } else {
            sourceLabel = "Khác";
        }

        return UserVoucherDTO.builder()
                .id(uv.getId())
                .voucherId(v.getId())
                .code(v.getCode())
                .name(v.getName() != null ? v.getName() : v.getCode())
                .description(v.getDescription())
                .type(v.getType() != null ? v.getType().name() : null)
                .discountValue(v.getDiscountValue())
                .minOrderValue(v.getMinOrderValue())
                .expireAt(effectiveExpireAt != null ? effectiveExpireAt.format(FORMATTER) : null)
                .isUsed(Boolean.TRUE.equals(uv.getIsUsed()))
                .usedAt(uv.getUsedAt() != null ? uv.getUsedAt().format(FORMATTER) : null)
                .receivedAt(uv.getReceivedAt() != null ? uv.getReceivedAt().format(FORMATTER) : null)
                .source(uv.getSource())
                .sourceLabel(sourceLabel)
                .isExpired(isExpired)
                .displayValue(displayValue)
                .build();
    }

    private static String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "0đ";
        return String.format("%,.0fđ", amount);
    }
}
