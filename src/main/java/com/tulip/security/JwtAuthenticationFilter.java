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
        
        log.info("JwtAuthenticationFilter: Filter called for path: {}", request.getRequestURI());
        
        String jwt = null;
        String username = null;
        
        // 1. Ưu tiên đọc từ Authorization header (cho API)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug("JWT found in Authorization header for path: {}", request.getRequestURI());
        } 
        // 2. Nếu không có header, đọc từ cookie (cho web)
        else {
            Cookie[] cookies = request.getCookies();
            log.info("JwtAuthenticationFilter: Reading cookies for path: {}, cookies count: {}", 
                    request.getRequestURI(), cookies != null ? cookies.length : 0);
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    log.info("JwtAuthenticationFilter: Cookie found: name={}, value length={}", 
                            cookie.getName(), cookie.getValue() != null ? cookie.getValue().length() : 0);
                    if ("accessToken".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        log.info("JwtAuthenticationFilter: AccessToken cookie found, length: {}", jwt != null ? jwt.length() : 0);
                        break;
                    }
                }
            }
            
            if (jwt == null) {
                log.warn("JwtAuthenticationFilter: No accessToken found in cookies for path: {}", request.getRequestURI());
            }
        }
        
        // 3. Xác định loại endpoint
        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.equals("/login") || path.equals("/register") || path.equals("/logout");
        boolean isApiEndpoint = path.startsWith("/v1/api/auth") || path.startsWith("/v1/api/store");
        boolean isStaticResource = path.startsWith("/static") || path.startsWith("/css") || 
                                   path.startsWith("/js") || path.startsWith("/images") ||
                                   path.startsWith("/h2-console");
        boolean isPublicWebEndpoint = path.equals("/") || path.startsWith("/products") || 
                                     path.startsWith("/product") || path.startsWith("/trending") ||
                                     path.startsWith("/sale") || path.startsWith("/about") ||
                                     path.startsWith("/contact");
        
        // 4. Skip hoàn toàn các endpoint không cần authentication
        if (isAuthEndpoint || isApiEndpoint || isStaticResource) {
            log.debug("JwtAuthenticationFilter: Skipping endpoint (no auth needed): {}", path);
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
                    log.warn("Invalid token type: {}", tokenType);
                    // Nếu là public web endpoint và token không hợp lệ, vẫn cho phép truy cập
                    if (isPublicWebEndpoint) {
                        log.debug("JwtAuthenticationFilter: Public endpoint with invalid token, allowing access: {}", path);
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
                    log.debug("JwtAuthenticationFilter: Public endpoint with JWT parse error, allowing access: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }
        
        // 6. Set authentication vào SecurityContext nếu có JWT hợp lệ
        // Lưu ý: Ngay cả trên public endpoints, nếu có JWT hợp lệ thì vẫn set authentication
        // để Thymeleaf có thể hiển thị thông tin user (ví dụ: tên user, logout button)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.info("Setting authentication for user: {} on path: {} (public endpoint: {})", 
                    username, request.getRequestURI(), isPublicWebEndpoint);
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
                    log.debug("Authentication set successfully for user: {}", username);
            } else {
                // Access token hết hạn - thử refresh tự động
                log.info("Access token expired, attempting to refresh...");
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
                        log.info("Access token refreshed successfully for user: {}", username);
                    }
                } else {
                    log.warn("Failed to refresh access token for user: {}", username);
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
                log.debug("No refresh token found in cookies");
                return null;
            }
            
            // Validate refresh token
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                log.debug("Refresh token is invalid or expired");
                return null;
            }
            
            // Kiểm tra trong database
            RefreshToken tokenEntity = refreshTokenService.findByToken(refreshToken)
                .orElse(null);
            
            if (tokenEntity == null || !tokenEntity.isValid()) {
                log.debug("Refresh token not found in database or revoked");
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
            log.debug("New accessToken cookie set after refresh");
            
            return newAccessToken;
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return null;
        }
    }
}

