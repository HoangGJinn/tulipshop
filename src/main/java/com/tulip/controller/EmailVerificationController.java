package com.tulip.controller;

import com.tulip.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class EmailVerificationController {
    private final UserService userService;

    // GET endpoint để hiển thị trang verify-email (giữ lại cho view)
    @GetMapping("/verify-email")
    public String showVerifyEmailPage(@RequestParam("email") String email,
                                     @RequestParam(value = "unverified", required = false) String unverified,
                                     Model model) {
        model.addAttribute("email", email);
        model.addAttribute("form", new VerifyOtpForm());
        if (unverified != null) {
            model.addAttribute("error", "Vui lòng xác thực email trước khi đăng nhập!");
        }
        return "verify-email";
    }

    // API endpoint để verify OTP - trả về JSON
    @PostMapping("/v1/api/auth/verify-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody VerifyOtpRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate input
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Mã OTP không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!request.getOtp().matches("^[0-9]{6}$")) {
            response.put("success", false);
            response.put("message", "Mã OTP phải là 6 chữ số");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean isValid = userService.verifyEmail(request.getEmail().trim(), request.getOtp().trim());
            if (isValid) {
                response.put("success", true);
                response.put("message", "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // API endpoint để gửi lại OTP - trả về JSON
    @PostMapping("/v1/api/auth/resend-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody ResendOtpRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate input
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            userService.resendOtp(request.getEmail().trim());
            response.put("success", true);
            response.put("message", "Đã gửi lại mã OTP. Vui lòng kiểm tra email của bạn.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DTO cho verify OTP request
    @Data
    @NoArgsConstructor
    public static class VerifyOtpRequest {
        private String email;
        private String otp;
    }

    // DTO cho resend OTP request
    @Data
    @NoArgsConstructor
    public static class ResendOtpRequest {
        private String email;
    }

    // Form class giữ lại cho view (GET /verify-email)
    @Data
    @NoArgsConstructor
    public static class VerifyOtpForm {
        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải là 6 chữ số")
        private String otp;
    }
}


