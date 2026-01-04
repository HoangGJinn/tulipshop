package com.tulip.controller.api;

import com.tulip.dto.VoucherApplyRequestDTO;
import com.tulip.dto.VoucherApplyResponseDTO;
import com.tulip.dto.VoucherDTO;
import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/v1/api/vouchers")
@RequiredArgsConstructor
public class VoucherApiController {

    private final VoucherService voucherService;

    @GetMapping("/applicable")
    public List<VoucherDTO> getApplicableVouchers(@RequestParam BigDecimal orderTotal) {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        // Chỉ lấy voucher public, còn hạn, còn lượt, đủ điều kiện
        List<VoucherDTO> applicable = vouchers.stream()
                .filter(v -> v.getIsPublic() == null || Boolean.TRUE.equals(v.getIsPublic()))
                .filter(v -> voucherService.isValid(v.getCode(), orderTotal))
                .map(VoucherDTO::fromEntity)
                .toList();

        // Ưu tiên voucher có lợi nhất cho người dùng (giảm nhiều nhất)
        applicable.sort((a, b) -> {
            // Ưu tiên voucher giảm tiền > freeship, rồi đến phần trăm
            if ("FREESHIP".equals(a.getType()) && !"FREESHIP".equals(b.getType()))
                return 1;
            if (!"FREESHIP".equals(a.getType()) && "FREESHIP".equals(b.getType()))
                return -1;
            // Nếu cùng loại, so sánh discountValue giảm dần
            int cmp = b.getDiscountValue() != null && a.getDiscountValue() != null
                    ? b.getDiscountValue().compareTo(a.getDiscountValue())
                    : 0;
            if (cmp != 0)
                return cmp;
            // Nếu cùng giá trị, ưu tiên voucher còn nhiều lượt hơn
            return Integer.compare(b.getRemaining() != null ? b.getRemaining() : 0,
                    a.getRemaining() != null ? a.getRemaining() : 0);
        });
        return applicable;
    }

    /**
     * Lấy tất cả voucher cho trang checkout (bao gồm cả không hợp lệ để hiển thị
     * mờ)
     */
    @GetMapping("/checkout-vouchers")
    public List<VoucherDTO> getCheckoutVouchers(@RequestParam BigDecimal orderTotal) {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        LocalDateTime now = LocalDateTime.now();

        List<VoucherDTO> result = new ArrayList<>();

        for (Voucher v : vouchers) {
            // Chỉ lấy voucher public và đang active
            if (v.getIsPublic() != null && !v.getIsPublic())
                continue;
            if (!Boolean.TRUE.equals(v.getStatus()))
                continue;

            // Check ngày bắt đầu
            if (v.getStartAt() != null && now.isBefore(v.getStartAt()))
                continue;

            // Check ngày hết hạn
            if (v.getExpireAt() != null && now.isAfter(v.getExpireAt()))
                continue;

            VoucherDTO dto = VoucherDTO.fromEntity(v);

            // Tính discount thực tế
            BigDecimal calculatedDiscount = voucherService.calculateDiscount(v.getCode(), orderTotal);
            dto.setCalculatedDiscount(calculatedDiscount);

            // Kiểm tra điều kiện áp dụng
            boolean canApply = true;
            String reason = null;

            // Check min order value
            if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) {
                canApply = false;
                reason = "Đơn hàng tối thiểu " + formatCurrency(v.getMinOrderValue());
            }

            // Check quantity
            if (canApply && v.getQuantity() != null && v.getQuantity() > 0) {
                int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                if (used >= v.getQuantity()) {
                    canApply = false;
                    reason = "Đã hết lượt sử dụng";
                }
            }

            dto.setCanApply(canApply);
            dto.setDisabledReason(reason);

            result.add(dto);
        }

        // Sắp xếp: voucher hợp lệ lên trước, sau đó theo discount giảm dần
        result.sort(Comparator
                .comparing(VoucherDTO::getCanApply, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(v -> v.getCalculatedDiscount() != null ? v.getCalculatedDiscount() : BigDecimal.ZERO,
                        Comparator.reverseOrder()));

        return result;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "0đ";
        return String.format("%,.0fđ", amount);
    }

    @PostMapping("/apply")
    public VoucherApplyResponseDTO applyVoucher(@RequestBody VoucherApplyRequestDTO request) {
        BigDecimal discount = voucherService.calculateDiscount(request.getCode(), request.getOrderTotal());
        boolean valid = voucherService.isValid(request.getCode(), request.getOrderTotal());
        return VoucherApplyResponseDTO.builder()
                .success(valid)
                .discountAmount(discount)
                .message(valid ? "Applied" : "Invalid")
                .build();
    }
}
