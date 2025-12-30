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
        List<Voucher> vouchers = voucherService.getApplicableVouchers(orderTotal);
        
        List<VoucherDTO> voucherDTOs = vouchers.stream().map(v -> {
            Integer remaining = v.getQuantity() != null && v.getUsedCount() != null
                    ? v.getQuantity() - v.getUsedCount()
                    : 0;
            
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
                    .isValid(v.isValid())
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
        VoucherApplyResponseDTO response = voucherService.calculateDiscount(request);
        return ResponseEntity.ok(response);
    }
}

