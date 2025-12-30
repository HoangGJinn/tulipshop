package com.tulip.security;

import com.tulip.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interceptor để xác thực JWT token khi kết nối WebSocket
 * Trích xuất JWT từ header và gán Principal cho phiên WebSocket
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
    
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                // Lấy JWT token từ header
                // Client sẽ gửi token qua header "Authorization" hoặc "X-Authorization"
                String token = extractToken(accessor);
                
                if (token != null) {
                    // Validate và extract username từ token
                    String username = jwtUtil.extractUsername(token);
                    String tokenType = jwtUtil.extractTokenType(token);
                    
                    // Chỉ chấp nhận ACCESS token
                    if ("ACCESS".equals(tokenType) && username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        // Validate token
                        if (jwtUtil.validateToken(token, userDetails)) {
                            // Tạo Authentication object
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                                );
                            
                            // Gán Principal cho phiên WebSocket
                            // Điều này cho phép gửi message đến user cụ thể qua /user/{username}/queue/...
                            accessor.setUser(authentication);
                            
                            log.info("WebSocket authenticated successfully for user: {}", username);
                        } else {
                            log.warn("Invalid JWT token for WebSocket connection");
                        }
                    } else {
                        log.warn("Invalid token type for WebSocket: {}", tokenType);
                    }
                } else {
                    log.warn("No JWT token found in WebSocket CONNECT frame");
                }
            } catch (Exception e) {
                log.error("Error authenticating WebSocket connection: {}", e.getMessage());
            }
        }
        
        return message;
    }
    
    /**
     * Trích xuất JWT token từ STOMP headers
     * Hỗ trợ nhiều cách gửi token:
     * 1. Authorization: Bearer <token>
     * 2. X-Authorization: <token>
     * 3. token: <token>
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Cách 1: Authorization header với Bearer prefix
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        
        // Cách 2: X-Authorization header (không có Bearer prefix)
        List<String> xAuthHeaders = accessor.getNativeHeader("X-Authorization");
        if (xAuthHeaders != null && !xAuthHeaders.isEmpty()) {
            return xAuthHeaders.get(0);
        }
        
        // Cách 3: token header trực tiếp
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        
        return null;
    }
}
