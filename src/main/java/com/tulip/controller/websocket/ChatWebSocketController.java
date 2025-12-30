package com.tulip.controller.websocket;

import com.tulip.dto.ChatMessageDTO;
import com.tulip.dto.ChatTypingDTO;
import com.tulip.entity.enums.MessageType;
import com.tulip.repository.UserRepository;
import com.tulip.service.ChatService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket Controller cho Chat
 * 
 * Lưu ý bảo mật:
 * - KHÔNG tin tưởng senderId từ client
 * - Lấy userId từ WebSocket session (đã được xác thực trong interceptor)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Gửi tin nhắn
     * Destination: /app/chat/send
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        
        // Lấy user từ WebSocket session (đã được xác thực)
        Long userId = getUserIdFromSession(headerAccessor);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());
        String content = payload.get("content").toString();
        String typeStr = payload.getOrDefault("type", "TEXT").toString();
        MessageType type = MessageType.valueOf(typeStr);
        
        // Gửi tin nhắn (service sẽ kiểm tra quyền)
        chatService.sendMessage(chatRoomId, user, content, type);
        
        log.debug("Message sent: RoomID={}, UserID={}, Type={}", 
            chatRoomId, userId, type);
    }

    /**
     * Gửi typing indicator
     * Destination: /app/chat/typing
     * Không lưu DB
     */
    @MessageMapping("/chat/typing")
    public void sendTyping(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        
        Long userId = getUserIdFromSession(headerAccessor);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());
        Boolean isTyping = Boolean.valueOf(payload.get("isTyping").toString());
        
        // Gửi typing indicator (không lưu DB)
        chatService.sendTypingIndicator(chatRoomId, user, isTyping);
    }

    /**
     * Join vào phòng chat
     * Destination: /app/chat/join
     */
    @MessageMapping("/chat/join")
    public void joinRoom(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        
        Long userId = getUserIdFromSession(headerAccessor);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());
        
        // Gửi tin nhắn JOIN (hệ thống)
        chatService.sendMessage(chatRoomId, user, 
            user.getProfile() != null ? user.getProfile().getFullName() : user.getEmail() + " đã tham gia",
            MessageType.JOIN);
        
        log.debug("User joined chat room: RoomID={}, UserID={}", chatRoomId, userId);
    }

    /**
     * Lấy userId từ WebSocket session
     * Đã được xác thực trong WebSocketAuthInterceptor
     */
    private Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new RuntimeException("Session attributes not found");
        }
        
        Object userIdObj = sessionAttributes.get("userId");
        if (userIdObj == null) {
            // Thử lấy từ authentication
            Authentication auth = (Authentication) sessionAttributes.get("authentication");
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
                return ((CustomUserDetails) auth.getPrincipal()).getUserId();
            }
            throw new RuntimeException("User ID not found in session");
        }
        
        return Long.valueOf(userIdObj.toString());
    }
}

