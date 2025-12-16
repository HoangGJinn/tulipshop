package com.tulip.security;

import com.tulip.service.RefreshTokenService;
import com.tulip.service.impl.CustomOAuth2User;
import com.tulip.service.impl.CustomUserDetails;
import com.tulip.service.impl.CustomUserDetailsService;
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
public class JwtOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;
    
    // Constructor để inject dependencies
    public JwtOAuth2SuccessHandler(JwtUtil jwtUtil, RefreshTokenService refreshTokenService, 
                                   CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
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
        
        if (principal instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) principal;
            Long userId = oauth2User.getUserId();
            String email = oauth2User.getEmail();
            
            // Convert OAuth2User sang UserDetails để tạo JWT
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
            
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
            
            // OAuth2 users đã được Google xác thực email rồi, không cần kiểm tra verify
        } else {
            log.warn("JwtOAuth2SuccessHandler: Principal is not CustomOAuth2User, type: {}", 
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

