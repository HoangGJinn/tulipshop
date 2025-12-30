package com.tulip.controller.api;

import com.tulip.dto.ChatMessageDTO;
import com.tulip.dto.ChatRoomDTO;
import com.tulip.entity.enums.MessageType;
import com.tulip.repository.UserRepository;
import com.tulip.service.ChatService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API cho Chat
 * 
 * Lưu ý: Lịch sử chat nên lấy bằng REST API, không dùng WebSocket
 * WebSocket chỉ dùng cho real-time messaging
 */
@Slf4j
@RestController
@RequestMapping("/v1/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Tạo hoặc lấy phòng chat của khách hàng
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDTO> createOrGetChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        var room = chatService.createOrGetChatRoom(user);
        return ResponseEntity.ok(chatService.getChatRoomDTO(room.getId(), user));
    }

    /**
     * Lấy danh sách phòng chat của khách hàng
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getCustomerChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(chatService.getCustomerChatRooms(user));
    }

    /**
     * Lấy thông tin phòng chat
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDTO> getChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(chatService.getChatRoomDTO(roomId, user));
    }

    /**
     * Lấy lịch sử tin nhắn (REST API, không dùng WebSocket)
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageDTO>> getChatHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(chatService.getChatHistory(roomId, user, pageable));
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    @PostMapping("/rooms/{roomId}/seen")
    public ResponseEntity<Void> markAsSeen(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        chatService.markMessagesAsSeen(roomId, user);
        return ResponseEntity.ok().build();
    }

    /**
     * Lấy số tin nhắn chưa đọc
     */
    @GetMapping("/rooms/{roomId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(chatService.getUnreadCount(roomId, user));
    }
}

