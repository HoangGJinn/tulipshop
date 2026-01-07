package com.tulip.controller.api;

import com.tulip.dto.response.UserVoucherDTO;
import com.tulip.service.impl.CustomUserDetails;
import com.tulip.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/user/vouchers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserVoucherApiController {

    private final UserVoucherService userVoucherService;

    /**
     * Lấy danh sách voucher khả dụng của user hiện tại
     */
    @GetMapping
    public ResponseEntity<List<UserVoucherDTO>> getMyVouchers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userVoucherService.getAvailableVouchers(userDetails.getUserId()));
    }

    /**
     * Lấy tất cả voucher của user (bao gồm đã dùng)
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserVoucherDTO>> getAllMyVouchers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userVoucherService.getAllVouchers(userDetails.getUserId()));
    }

    /**
     * Kiểm tra voucher có thể sử dụng không
     */
    @GetMapping("/check/{code}")
    public ResponseEntity<?> checkVoucher(
            @PathVariable String code,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(Map.of("canUse", false, "message", "Vui lòng đăng nhập"));
        }

        boolean canUse = userVoucherService.canUseVoucher(userDetails.getUserId(), code);
        return ResponseEntity.ok(Map.of("canUse", canUse));
    }
}
