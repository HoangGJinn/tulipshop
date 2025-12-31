package com.tulip.controller;

import com.tulip.dto.response.ChatMessageResponseDTO;
import com.tulip.dto.response.ChatSessionResponseDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatSession;
import com.tulip.repository.UserRepository;
import com.tulip.service.ChatService;
import com.tulip.service.impl.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    private User getUserFromPrincipal(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails custom) {
            return userRepository.findById(custom.getUserId()).orElseThrow();
        }
        throw new RuntimeException("Unauthorized");
    }

    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> createSession(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        ChatSession session = chatService.createSession(user, null, null);
        return ResponseEntity.ok(Map.of("sessionId", session.getId(), "sessionToken", session.getSessionToken()));
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getOrCreateSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String sessionToken) {
        User user = getUserFromPrincipal(userDetails);
        ChatSession session = chatService.getSessionOrCreate(user, sessionToken);
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "sessionToken", session.getSessionToken(),
                "messages", chatService.getSessionMessages(session.getId())
        ));
    }

    @PostMapping("/session/{sessionId}/message")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @PathVariable Long sessionId, @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(chatService.sendCustomerMessage(sessionId, request.get("content")));
    }

    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }
}