package com.tulip.security;

import com.tulip.entity.RefreshToken;
import com.tulip.service.RefreshTokenService;
import com.tulip.service.impl.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String jwt = null;
        String username = null;
        
        // 1. Ưu tiên đọc từ Authorization header (cho API)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } 
        // 2. Nếu không có header, đọc từ cookie (cho web)
        else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        // 3. Xác định loại endpoint
        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.equals("/login") || path.equals("/register") || path.equals("/logout") 
                || path.equals("/forgot-password") || path.equals("/reset-password");
        boolean isPublicApiEndpoint = path.startsWith("/v1/api/auth/login") || 
                                     path.startsWith("/v1/api/auth/register") ||
                                     path.startsWith("/v1/api/auth/forgot-password") ||
                                     path.startsWith("/v1/api/auth/reset-password") ||
                                     path.startsWith("/v1/api/auth/resend-otp") ||
                                     path.startsWith("/v1/api/auth/verify-email") ||
                                     path.startsWith("/v1/api/store");
        boolean isStaticResource = path.startsWith("/static") || path.startsWith("/css") || 
                                   path.startsWith("/js") || path.startsWith("/images") ||
                                   path.startsWith("/h2-console");
        boolean isPublicWebEndpoint = path.equals("/") || path.startsWith("/products") || 
                                     path.startsWith("/product") || path.startsWith("/trending") ||
                                     path.startsWith("/sale") || path.startsWith("/about") ||
                                     path.startsWith("/contact");
        
        // 4. Skip hoàn toàn các endpoint không cần authentication
        if (isAuthEndpoint || isPublicApiEndpoint || isStaticResource) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 5. Validate và extract username từ JWT (nếu có)
        if (jwt != null) {
            try {
                username = jwtUtil.extractUsername(jwt);
                
                // Chỉ xử lý ACCESS token
                String tokenType = jwtUtil.extractTokenType(jwt);
                if (!"ACCESS".equals(tokenType)) {
                    // Nếu là public web endpoint và token không hợp lệ, vẫn cho phép truy cập
                    if (isPublicWebEndpoint) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                log.error("JWT parsing error: {}", e.getMessage());
                // Nếu là public web endpoint và JWT parse lỗi, vẫn cho phép truy cập
                if (isPublicWebEndpoint) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }
        
        // 6. Set authentication vào SecurityContext nếu có JWT hợp lệ
        // Lưu ý: Ngay cả trên public endpoints, nếu có JWT hợp lệ thì vẫn set authentication
        // để Thymeleaf có thể hiển thị thông tin user (ví dụ: tên user, logout button)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Role có thể được lấy từ JWT: jwtUtil.extractRole(jwt)
            // Nhưng vẫn sử dụng authorities từ UserDetails để đảm bảo tính nhất quán
            
            if (jwtUtil.validateToken(jwt, userDetails)) {
                // Access token còn hợp lệ
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // Access token hết hạn - thử refresh tự động
                jwt = tryRefreshToken(request, response, username);
                
                if (jwt != null) {
                    // Refresh thành công, validate lại
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                            );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Tự động refresh access token nếu refresh token còn hợp lệ
     * @return Access token mới hoặc null nếu không thể refresh
     */
    private String tryRefreshToken(HttpServletRequest request, HttpServletResponse response, String username) {
        try {
            // Lấy refresh token từ cookie
            Cookie[] cookies = request.getCookies();
            String refreshToken = null;
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
            
            if (refreshToken == null) {
                return null;
            }
            
            // Validate refresh token
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                return null;
            }
            
            // Kiểm tra trong database
            RefreshToken tokenEntity = refreshTokenService.findByToken(refreshToken)
                .orElse(null);
            
            if (tokenEntity == null || !tokenEntity.isValid()) {
                return null;
            }
            
            // Lấy thông tin user
            Long userId = jwtUtil.extractUserId(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Tạo access token mới
            String newAccessToken = jwtUtil.generateAccessToken(userDetails, userId);
            
            // Cập nhật cookie với access token mới (có SameSite attribute)
            ResponseCookie newAccessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                    .path("/")
                    .maxAge(accessTokenExpiration.intValue())
                    .httpOnly(true)
                    .secure(false) // Set true nếu dùng HTTPS
                    .sameSite("Lax")
                    .build();
            
            response.addHeader("Set-Cookie", newAccessTokenCookie.toString());
            
            return newAccessToken;
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return null;
        }
    }
}

