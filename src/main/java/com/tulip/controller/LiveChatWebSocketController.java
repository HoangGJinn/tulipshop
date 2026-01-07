package com.tulip.controller;

import com.tulip.dto.LiveChatMessageDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;
import com.tulip.repository.UserRepository;
import com.tulip.service.LiveChatService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Controller cho Live Chat
 * Xử lý tin nhắn real-time qua WebSocket
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LiveChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final LiveChatService liveChatService;
    private final UserRepository userRepository;
    
    /**
     * Nhận tin nhắn từ client (khách hàng hoặc staff)
     * Client gửi đến: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload, Authentication authentication) {
        try {
            String sessionToken = (String) payload.get("sessionToken");
            String content = (String) payload.get("content");
            String senderTypeStr = (String) payload.get("senderType");
            
            if (sessionToken == null || content == null || content.trim().isEmpty()) {
                log.warn("Invalid message payload: {}", payload);
                return;
            }
            
            // Lấy session
            ChatSession session = liveChatService.getSessionByToken(sessionToken)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            
            // Xác định sender
            User sender = null;
            ChatMessage.SenderType senderType;
            
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                if (userDetails instanceof CustomUserDetails custom) {
                    sender = userRepository.findById(custom.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    
                    // Xác định senderType dựa trên role
                    if (sender.getRole().name().equals("STAFF") || sender.getRole().name().equals("ADMIN")) {
                        senderType = ChatMessage.SenderType.SUPPORT_AGENT;
                    } else {
                        senderType = ChatMessage.SenderType.CUSTOMER;
                    }
                } else {
                    senderType = ChatMessage.SenderType.CUSTOMER;
                }
            } else {
                // User chưa đăng nhập (không nên xảy ra với live chat - yêu cầu đăng nhập)
                log.warn("Unauthenticated user attempting to send message in live chat");
                senderType = ChatMessage.SenderType.CUSTOMER;
            }
            
            // Override senderType nếu được chỉ định trong payload
            if (senderTypeStr != null) {
                try {
                    senderType = ChatMessage.SenderType.valueOf(senderTypeStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid senderType: {}", senderTypeStr);
                }
            }
            
            // Lưu tin nhắn vào database
            LiveChatMessageDTO savedMessage = liveChatService.saveMessage(
                    session.getId(), 
                    content, 
                    senderType, 
                    sender
            );
            
            log.info("✅ Message saved to DB: id={}, sessionId={}, senderType={}", 
                savedMessage.getId(), savedMessage.getSessionId(), savedMessage.getSenderType());
            
            // Gửi tin nhắn đến kênh riêng của session này
            String destination = "/topic/chat/" + sessionToken;
            messagingTemplate.convertAndSend(destination, savedMessage);
            log.info("📤 Đã gửi tin nhắn đến {}", destination);
            
            // Nếu người gửi là CUSTOMER, bắn thông báo cho ADMIN/STAFF
            if (senderType == ChatMessage.SenderType.CUSTOMER) {
                // Gửi đến topic admin để thông báo có tin nhắn mới
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "NEW_MESSAGE");
                notification.put("sessionId", session.getId());
                notification.put("sessionToken", sessionToken);
                notification.put("customerName", session.getCustomerName());
                notification.put("message", savedMessage);
                
                messagingTemplate.convertAndSend("/topic/admin/chat-notifications", notification);
                log.info("📢 Đã thông báo tin nhắn mới cho admin");
            }
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý tin nhắn: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Staff đánh dấu đã đọc tin nhắn
     * Client gửi đến: /app/chat.markAsRead
     */
    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload Map<String, Object> payload, Authentication authentication) {
        try {
            String sessionToken = (String) payload.get("sessionToken");
            
            if (sessionToken == null) {
                return;
            }
            
            ChatSession session = liveChatService.getSessionByToken(sessionToken)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                if (userDetails instanceof CustomUserDetails custom) {
                    Long userId = custom.getUserId();
                    liveChatService.markMessagesAsRead(session.getId(), userId);
                    
                    // Thông báo cho customer rằng staff đã đọc
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "MESSAGES_READ");
                    notification.put("sessionToken", sessionToken);
                    
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionToken, notification);
                }
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi đánh dấu đã đọc: {}", e.getMessage(), e);
        }
    }
}

