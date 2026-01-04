package com.tulip.controller.api;

import com.tulip.dto.LiveChatMessageDTO;
import com.tulip.dto.LiveChatSessionDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatSession;
import com.tulip.repository.UserRepository;
import com.tulip.service.LiveChatService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho Live Chat
 */
@Slf4j
@RestController
@RequestMapping("/v1/api/live-chat")
@RequiredArgsConstructor
public class LiveChatApiController {
    
    private final LiveChatService liveChatService;
    private final UserRepository userRepository;
    
    private User getUserFromPrincipal(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails custom) {
            return userRepository.findById(custom.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        throw new RuntimeException("Unauthorized");
    }
    
    /**
     * Kiểm tra xem user đã đăng nhập chưa
     */
    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuth(
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        if (userDetails != null) {
            response.put("authenticated", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("authenticated", false);
            return ResponseEntity.status(401).body(response);
        }
    }
    
    /**
     * Tạo hoặc lấy session cho khách hàng (chỉ user đã đăng nhập)
     */
    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> getOrCreateSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String sessionToken) {
        
        try {
            // Chỉ cho phép user đã đăng nhập
            if (userDetails == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Unauthorized");
                errorResponse.put("message", "Vui lòng đăng nhập để sử dụng live chat");
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            User user = getUserFromPrincipal(userDetails);
            ChatSession session = liveChatService.getOrCreateSession(user, sessionToken);
            List<LiveChatMessageDTO> messages = liveChatService.getSessionMessages(session.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("sessionToken", session.getSessionToken());
            response.put("messages", messages);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating/retrieving live chat session", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tạo session chat");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Lấy danh sách tin nhắn của một session
     */
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<List<LiveChatMessageDTO>> getMessages(@PathVariable Long sessionId) {
        List<LiveChatMessageDTO> messages = liveChatService.getSessionMessages(sessionId);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Lấy tất cả session với filter (cho admin/staff)
     * @param status - NEW, PROCESSING, CLOSED, hoặc null (all)
     * @param fromDate - yyyy-MM-dd
     * @param toDate - yyyy-MM-dd
     */
    @GetMapping("/admin/sessions")
    public ResponseEntity<List<LiveChatSessionDTO>> getAllSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        User user = getUserFromPrincipal(userDetails);
        
        // Kiểm tra quyền
        if (!user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
            log.warn("Access denied for user {} with role {}", user.getEmail(), user.getRole().name());
            return ResponseEntity.status(403).build();
        }
        
        List<LiveChatSessionDTO> sessions = liveChatService.getAllSessionsWithFilter(status, fromDate, toDate);
        log.info("Admin {} loaded {} live chat sessions (status: {}, from: {}, to: {})", 
                 user.getEmail(), sessions.size(), status, fromDate, toDate);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Lấy danh sách session của một staff
     */
    @GetMapping("/staff/sessions")
    public ResponseEntity<List<LiveChatSessionDTO>> getStaffSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromPrincipal(userDetails);
        
        // Kiểm tra quyền
        if (!user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
            return ResponseEntity.status(403).build();
        }
        
        List<LiveChatSessionDTO> sessions = liveChatService.getStaffSessions(user.getId());
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Staff nhận xử lý một session
     */
    @PostMapping("/admin/session/{sessionId}/assign")
    public ResponseEntity<Map<String, Object>> assignSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromPrincipal(userDetails);
        
        // Kiểm tra quyền
        if (!user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
            return ResponseEntity.status(403).build();
        }
        
        ChatSession session = liveChatService.assignStaffToSession(sessionId, user.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("status", session.getStatus().name());
        response.put("staffId", session.getStaff() != null ? session.getStaff().getId() : null);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Đóng một session
     */
    @PostMapping("/admin/session/{sessionId}/close")
    public ResponseEntity<Map<String, Object>> closeSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromPrincipal(userDetails);
        
        // Kiểm tra quyền
        if (!user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
            return ResponseEntity.status(403).build();
        }
        
        ChatSession session = liveChatService.closeSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("status", session.getStatus().name());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Đánh dấu tin nhắn đã đọc (chỉ cho user đã đăng nhập)
     */
    @PutMapping("/session/{sessionId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            if (userDetails != null) {
                User user = getUserFromPrincipal(userDetails);
                liveChatService.markMessagesAsRead(sessionId, user.getId());
            } else {
                log.warn("Unauthenticated user attempting to mark messages as read for session: {}", sessionId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Đếm số tin nhắn chưa đọc
     */
    @GetMapping("/session/{sessionId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromPrincipal(userDetails);
        Long count = liveChatService.countUnreadMessages(sessionId, user.getId());
        
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        
        return ResponseEntity.ok(response);
    }
}

