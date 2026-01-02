package com.tulip.config;

import com.tulip.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket với STOMP protocol
 * Sử dụng JWT authentication qua JwtChannelInterceptor
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtChannelInterceptor jwtChannelInterceptor;
    
    /**
     * Cấu hình Message Broker
     * - /topic: Broadcast đến tất cả subscribers (public)
     * - /queue: Point-to-point messaging (private)
     * - /app: Prefix cho các message gửi đến server
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker
        // /topic cho broadcast, /queue cho point-to-point
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix cho các message từ client gửi đến server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho user-specific destinations
        // Khi gửi đến /user/{username}/queue/notifications
        // Client sẽ subscribe tại /user/queue/notifications
        config.setUserDestinationPrefix("/user");
    }
    
    /**
     * Đăng ký STOMP endpoint
     * Client sẽ kết nối đến endpoint này
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*") // Cho phép tất cả origins (có thể giới hạn trong production)
            .withSockJS(); // Enable SockJS fallback cho browsers không hỗ trợ WebSocket
    }
    
    /**
     * Cấu hình Channel Interceptor để xác thực JWT
     * Interceptor sẽ chạy trước khi message được xử lý
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
