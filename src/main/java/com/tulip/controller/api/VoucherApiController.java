package com.tulip.controller.api;

import com.tulip.dto.VoucherApplyRequestDTO;
import com.tulip.dto.VoucherApplyResponseDTO;
import com.tulip.dto.VoucherDTO;
import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/vouchers")
@RequiredArgsConstructor
public class VoucherApiController {

    private final VoucherService voucherService;

    /**
     * Lấy danh sách voucher có thể áp dụng cho đơn hàng
     */
    @GetMapping("/applicable")
    public ResponseEntity<List<VoucherDTO>> getApplicableVouchers(
            @RequestParam BigDecimal orderTotal) {
        // Get all vouchers and filter by order total
        List<Voucher> allVouchers = voucherService.getAllVouchers();
        List<Voucher> vouchers = allVouchers.stream()
                .filter(v -> {
                    // Check if voucher is valid for this order total
                    if (v.getStatus() == null || !v.getStatus()) return false;
                    if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) return false;
                    // Check date range
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (v.getStartAt() != null && now.isBefore(v.getStartAt())) return false;
                    if (v.getExpireAt() != null && now.isAfter(v.getExpireAt())) return false;
                    // Check quantity
                    if (v.getQuantity() != null && v.getUsedCount() != null && v.getQuantity() <= v.getUsedCount()) return false;
                    return true;
                })
                .collect(Collectors.toList());
        
        List<VoucherDTO> voucherDTOs = vouchers.stream().map(v -> {
            Integer remaining = v.getQuantity() != null && v.getUsedCount() != null
                    ? v.getQuantity() - v.getUsedCount()
                    : null;
            
            return VoucherDTO.builder()
                    .id(v.getId())
                    .code(v.getCode())
                    .type(v.getType().name())
                    .discountValue(v.getDiscountValue())
                    .minOrderValue(v.getMinOrderValue())
                    .quantity(v.getQuantity())
                    .usedCount(v.getUsedCount())
                    .remaining(remaining)
                    .startAt(v.getStartAt())
                    .expireAt(v.getExpireAt())
                    .status(v.getStatus())
                    .isValid(voucherService.isValid(v.getCode(), orderTotal))
                    .build();
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(voucherDTOs);
    }

    /**
     * Áp dụng voucher và tính toán giảm giá
     */
    @PostMapping("/apply")
    public ResponseEntity<VoucherApplyResponseDTO> applyVoucher(
            @RequestBody VoucherApplyRequestDTO request) {
        BigDecimal discountAmount = voucherService.calculateDiscount(request.getCode(), request.getOrderTotal());
        boolean isValid = voucherService.isValid(request.getCode(), request.getOrderTotal());
        
        VoucherApplyResponseDTO response = VoucherApplyResponseDTO.builder()
                .success(isValid)
                .discountAmount(discountAmount)
                .message(isValid ? "Voucher áp dụng thành công" : "Voucher không hợp lệ")
                .build();
        
        return ResponseEntity.ok(response);
    }
}

