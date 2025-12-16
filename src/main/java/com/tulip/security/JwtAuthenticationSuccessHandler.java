package com.tulip.security;

import com.tulip.service.RefreshTokenService;
import com.tulip.service.impl.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;
    
    // Constructor để inject dependencies
    public JwtAuthenticationSuccessHandler(JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }
    
    // Set default success URL sau khi @Value được inject
    @PostConstruct
    public void init() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Long userId = userDetails.getUserId();
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails, userId);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, userId);
            
            // Save refresh token to database
            refreshTokenService.createRefreshToken(userId, refreshToken);
            
            // Lưu vào HttpOnly Cookies với SameSite attribute
            if (accessTokenExpiration == null || refreshTokenExpiration == null) {
                log.error("JWT expiration values not initialized! accessTokenExpiration={}, refreshTokenExpiration={}", 
                        accessTokenExpiration, refreshTokenExpiration);
                throw new IllegalStateException("JWT expiration configuration not loaded");
            }
            addCookieToResponse(response, "accessToken", accessToken, accessTokenExpiration.intValue());
            addCookieToResponse(response, "refreshToken", refreshToken, refreshTokenExpiration.intValue());
        } else {
            log.warn("JwtAuthenticationSuccessHandler: Principal is not CustomUserDetails, type: {}", 
                    principal != null ? principal.getClass().getName() : "null");
        }
        
        // Redirect về trang chủ
        super.onAuthenticationSuccess(request, response, authentication);
    }
    
    /**
     * Tạo cookie với SameSite attribute để đảm bảo browser gửi cookie trong các request
     * Sử dụng ResponseCookie của Spring để hỗ trợ SameSite
     */
    private void addCookieToResponse(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(false) // Set true nếu dùng HTTPS
                .sameSite("Lax") // Quan trọng: Cho phép cookie được gửi trong same-site requests
                .build();
        
        response.addHeader("Set-Cookie", cookie.toString());
    }
}

