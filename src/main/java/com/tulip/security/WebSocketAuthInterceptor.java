package com.tulip.security;

import com.tulip.entity.User;
import com.tulip.repository.UserRepository;
import com.tulip.service.impl.CustomUserDetails;
import com.tulip.service.impl.CustomUserDetailsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor để xác thực WebSocket connection
 * 
 * Bảo mật quan trọng:
 * - Xác thực JWT/Session trước khi cho phép kết nối
 * - Gắn Principal vào WebSocket session
 * - KHÔNG tin tưởng senderId từ client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // Lấy token từ query parameter hoặc header
            String token = extractToken(servletRequest);

            if (token != null && jwtUtil.validateToken(token)) {
                try {
                    // Lấy thông tin user từ token
                    String email = jwtUtil.extractEmail(token);

                    // Load user từ database
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found: " + email));

                    // Tạo authentication object
                    CustomUserDetails userDetails = new CustomUserDetails(user);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // Lưu vào attributes để dùng trong WebSocket session
                    attributes.put("user", userDetails);
                    attributes.put("userId", userDetails.getUserId());
                    attributes.put("authentication", authentication);

                    log.info("WebSocket authenticated for user: {} (ID: {})",
                            email, userDetails.getUserId());

                    return true;
                } catch (Exception e) {
                    log.error("Error authenticating WebSocket connection: {}", e.getMessage());
                    return false;
                }
            } else {
                // Thử lấy từ HTTP session (cho trường hợp đăng nhập bằng session)
                HttpSession session = servletRequest.getServletRequest().getSession(false);
                if (session != null) {
                    Object contextObj = session.getAttribute("SPRING_SECURITY_CONTEXT"); // Use string key directly or
                                                                                         // verify constant
                    // Or typically "SPRING_SECURITY_CONTEXT" is the key.
                    // Let's import SecurityContext
                    if (contextObj instanceof org.springframework.security.core.context.SecurityContext) {
                        org.springframework.security.core.context.SecurityContext context = (org.springframework.security.core.context.SecurityContext) contextObj;
                        Authentication auth = context.getAuthentication();
                        if (auth != null && auth.isAuthenticated()) {
                            attributes.put("user", auth.getPrincipal());
                            attributes.put("authentication", auth);

                            // Also set userId if principal is CustomUserDetails
                            if (auth.getPrincipal() instanceof CustomUserDetails) {
                                attributes.put("userId", ((CustomUserDetails) auth.getPrincipal()).getUserId());
                            }
                            return true;
                        }
                    }
                }

                log.warn("WebSocket connection rejected: Invalid or missing authentication");
                return false;
            }
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }

    private String extractToken(ServletServerHttpRequest request) {
        // Thử lấy từ query parameter
        String token = request.getServletRequest().getParameter("token");
        if (token != null) {
            return token;
        }

        // Thử lấy từ header Authorization
        String authHeader = request.getServletRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
