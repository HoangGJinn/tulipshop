package com.tulip.controller.api;

import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminVoucherApiController {

    private final VoucherService voucherService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllVouchers() {
        List<Voucher> vouchers = voucherService.getAllVouchers();

        List<Map<String, Object>> result = vouchers.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("code", v.getCode());
            map.put("type", v.getType() != null ? v.getType().name() : "N/A");
            map.put("typeDisplay", v.getType() == Voucher.DiscountType.PERCENT ? "Phần trăm" : "Số tiền");
            map.put("discountValue", v.getDiscountValue());
            map.put("minOrderValue", v.getMinOrderValue());
            map.put("quantity", v.getQuantity());
            map.put("usedCount", v.getUsedCount());
            map.put("remaining", v.getQuantity() != null && v.getUsedCount() != null
                    ? v.getQuantity() - v.getUsedCount()
                    : 0);
            map.put("startAt", v.getStartAt() != null ? v.getStartAt().format(FORMATTER) : "N/A");
            map.put("expireAt", v.getExpireAt() != null ? v.getExpireAt().format(FORMATTER) : "Không giới hạn");
            map.put("status", v.getStatus());
            map.put("isValid", v.isValid());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Voucher> getVoucherById(@PathVariable Long id) {
        return voucherService.getVoucherById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createVoucher(@RequestBody VoucherRequest request) {
        try {
            // Kiểm tra mã voucher đã tồn tại chưa
            if (voucherService.getVoucherByCode(request.code()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mã voucher đã tồn tại"));
            }

            Voucher voucher = Voucher.builder()
                    .code(request.code().toUpperCase())
                    .type(Voucher.DiscountType.valueOf(request.type()))
                    .discountValue(request.discountValue())
                    .minOrderValue(request.minOrderValue())
                    .quantity(request.quantity())
                    .usedCount(0)
                    .startAt(request.startAt())
                    .expireAt(request.expireAt())
                    .status(request.status() != null ? request.status() : true)
                    .build();

            Voucher saved = voucherService.saveVoucher(voucher);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(@PathVariable Long id, @RequestBody VoucherRequest request) {
        try {
            return voucherService.getVoucherById(id).map(voucher -> {
                // Kiểm tra nếu đổi mã voucher thì mã mới phải chưa tồn tại
                if (!voucher.getCode().equalsIgnoreCase(request.code())) {
                    if (voucherService.getVoucherByCode(request.code()).isPresent()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Mã voucher đã tồn tại"));
                    }
                }

                voucher.setCode(request.code().toUpperCase());
                voucher.setType(Voucher.DiscountType.valueOf(request.type()));
                voucher.setDiscountValue(request.discountValue());
                voucher.setMinOrderValue(request.minOrderValue());
                voucher.setQuantity(request.quantity());
                voucher.setStartAt(request.startAt());
                voucher.setExpireAt(request.expireAt());
                voucher.setStatus(request.status());

                Voucher saved = voucherService.saveVoucher(voucher);
                return ResponseEntity.ok(saved);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long id) {
        try {
            if (voucherService.getVoucherById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            voucherService.deleteVoucher(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa voucher thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DTO Record for request
    public record VoucherRequest(
            String code,
            String type,
            java.math.BigDecimal discountValue,
            java.math.BigDecimal minOrderValue,
            Integer quantity,
            @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startAt,
            @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime expireAt,
            Boolean status) {
    }
}
