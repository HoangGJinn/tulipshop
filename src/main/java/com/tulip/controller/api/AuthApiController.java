package com.tulip.controller.api;

import com.tulip.entity.RefreshToken;
import com.tulip.security.JwtUtil;
import com.tulip.service.RefreshTokenService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
public class AuthApiController {
    
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    @PostMapping("/refresh")
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
    
    @PostMapping("/logout")
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

