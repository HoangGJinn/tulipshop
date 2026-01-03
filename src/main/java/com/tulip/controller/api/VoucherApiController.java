package com.tulip.controller.api;

import com.tulip.dto.VoucherApplyRequestDTO;
import com.tulip.dto.VoucherApplyResponseDTO;
import com.tulip.dto.VoucherDTO;
import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/api/vouchers")
@RequiredArgsConstructor
public class VoucherApiController {

    private final VoucherService voucherService;

    @GetMapping("/applicable")
    public List<VoucherDTO> getApplicableVouchers(@RequestParam BigDecimal orderTotal) {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        return vouchers.stream()
                .filter(v -> voucherService.isValid(v.getCode(), orderTotal))
                .map(VoucherDTO::fromEntity)
                .toList();
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
