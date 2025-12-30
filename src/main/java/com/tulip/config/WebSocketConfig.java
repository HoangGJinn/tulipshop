package com.tulip.config;

import com.tulip.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration với STOMP
 * 
 * Để scale tốt hơn, nên dùng Broker Relay (RabbitMQ hoặc Redis):
 * 
 * 1. Thêm dependency vào pom.xml:
 *    - RabbitMQ: spring-boot-starter-amqp
 *    - Redis: spring-boot-starter-data-redis
 * 
 * 2. Thay enableSimpleBroker bằng enableStompBrokerRelay:
 * 
 *    registry.enableStompBrokerRelay("/topic", "/queue")
 *        .setRelayHost("localhost")
 *        .setRelayPort(61613)  // RabbitMQ STOMP port
 *        .setClientLogin("guest")
 *        .setClientPasscode("guest");
 * 
 *    Hoặc với Redis:
 *    registry.enableStompBrokerRelay("/topic", "/queue")
 *        .setRelayHost("localhost")
 *        .setRelayPort(61613)
 *        .setSystemLogin("admin")
 *        .setSystemPasscode("admin");
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple broker - phù hợp cho tải thấp-trung bình
        // TODO: Chuyển sang enableStompBrokerRelay khi cần scale
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Prefix cho messages từ client đến server
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho WebSocket connection
        registry.addEndpoint("/ws-chat")
                .addInterceptors(webSocketAuthInterceptor) // Bảo mật WebSocket
                .setAllowedOriginPatterns("*") // Trong production nên giới hạn domain cụ thể
                .withSockJS(); // Fallback cho browsers không hỗ trợ WebSocket
        
        log.info("WebSocket endpoint registered at /ws-chat");
    }
}

