package com.tulip.controller;

import com.tulip.entity.RefreshToken;
import com.tulip.security.JwtUtil;
import com.tulip.service.RefreshTokenService;
import com.tulip.service.UserService;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;

    // Tưởng tượng model là 1 cái đĩa, mình bỏ dữ liệu vào đĩa và mang tới view
    // Rồi view (thằng thymeleaf) sẽ lấy dữ liệu từ đĩa ra dùng
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "disabled", required = false) String disabled,
                       @RequestParam(value = "oauth2Error", required = false) String oauth2Error,
                       @RequestParam(value = "logout", required = false) String logout,
                       @RequestParam(value = "success", required = false) String success,
                       @RequestParam(value = "unverified", required = false) String unverified,
                       Model model) {
        // Kiểm tra nếu đã đăng nhập thì redirect về trang chủ
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/";
        }
        
        if (error != null) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng!");
        }
        if (disabled != null) {
            model.addAttribute("error", "Tài khoản này đã bị khóa hoặc chưa được kích hoạt!");
        }
        if (oauth2Error != null) {
            model.addAttribute("error", "Đăng nhập bằng Google thất bại. Vui lòng thử lại!");
        }
        if (logout != null) {
            model.addAttribute("message", "Bạn đã đăng xuất thành công.");
        }
        if (success != null) {
            model.addAttribute("message", "Đăng ký thành công. Vui lòng đăng nhập để tiếp tục.");
        }
        if (unverified != null) {
            model.addAttribute("error", "Vui lòng xác thực email trước khi đăng nhập!");
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        // Kiểm tra nếu đã đăng nhập thì redirect về trang chủ
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/";
        }
        
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br, Model model, RedirectAttributes redirectAttributes) {
        if (br.hasErrors()) {
            return "register";
        }
        try {
            userService.register(
                    form.getEmail(),
                    form.getPassword(),
                    form.getFullName(),
                    form.getPhone()
            );
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã OTP.");
            return "redirect:/verify-email?email=" + form.getEmail();
        } catch (Exception ex){
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @Data
    @NoArgsConstructor
    public static class RegisterForm {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;
        
        @NotBlank(message = "Mật khẩu không được để trống")
        private String password;

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        private String confirmPassword;

        @NotBlank(message = "Họ và tên không được để trống")
        private String fullName;

        private String phone;

        @AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp")
        public boolean isPasswordMatch() {
            if (password == null || confirmPassword == null) {
                return false;
            }
            return password.equals(confirmPassword);
        }

    }
    
    // ========== API Endpoints ==========
    
    @PostMapping("/v1/api/auth/refresh")
    @ResponseBody
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // Validate refresh token format
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Refresh token không hợp lệ hoặc đã hết hạn"));
            }
            
            // Check in database
            RefreshToken tokenEntity = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại"));
            
            if (!tokenEntity.isValid()) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Refresh token đã bị thu hồi hoặc hết hạn"));
            }
            
            // Get user details
            String username = jwtUtil.extractUsername(refreshToken);
            Long userId = jwtUtil.extractUserId(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Generate new access token
            String newAccessToken = jwtUtil.generateAccessToken(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", accessTokenExpiration); // Đọc từ properties
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Không thể làm mới token: " + e.getMessage()));
        }
    }
    
    @PostMapping("/v1/api/auth/logout")
    @ResponseBody
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            refreshTokenService.revokeToken(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
        }
    }
    
    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
