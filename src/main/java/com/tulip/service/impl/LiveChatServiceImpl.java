package com.tulip.service.impl;

import com.tulip.dto.LiveChatMessageDTO;
import com.tulip.dto.LiveChatSessionDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;
import com.tulip.repository.ChatMessageRepository;
import com.tulip.repository.ChatSessionRepository;
import com.tulip.repository.UserRepository;
import com.tulip.service.LiveChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LiveChatServiceImpl implements LiveChatService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    public ChatSession getOrCreateSession(User user, String sessionToken) {
        // Chỉ cho phép user đã đăng nhập
        if (user == null) {
            throw new RuntimeException("Live chat chỉ dành cho khách hàng đã đăng nhập");
        }
        
        // Nếu có token, tìm session cũ
        if (sessionToken != null) {
            Optional<ChatSession> existing = chatSessionRepository.findBySessionToken(sessionToken);
            if (existing.isPresent()) {
                ChatSession session = existing.get();
                // Chỉ trả về nếu là live chat và chưa đóng
                if (session.getChatType() == ChatSession.ChatType.LIVE_CHAT 
                    && session.getStatus() != ChatSession.SessionStatus.CLOSED) {
                    log.info("Reusing existing live chat session: {}", session.getId());
                    return session;
                } else {
                    log.info("Session {} is CLOSED or not LIVE_CHAT, creating new session", session.getId());
                    // Session đã đóng hoặc không phải LIVE_CHAT - tạo mới
                }
            }
        }
        
        // Tạo session mới
        String newToken = UUID.randomUUID().toString();
        
        try {
            // Xác định customer name từ user profile
            String finalCustomerName;
            if (user.getProfile() != null && user.getProfile().getFullName() != null) {
                finalCustomerName = user.getProfile().getFullName();
            } else {
                finalCustomerName = user.getEmail();
            }
            
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .sessionToken(newToken)
                    .customerName(finalCustomerName)
                    .customerEmail(user.getEmail())
                    .status(ChatSession.SessionStatus.NEW)
                    .chatType(ChatSession.ChatType.LIVE_CHAT)
                    .build();
            
            log.info("Building session with chatType: {}, status: {}", session.getChatType(), session.getStatus());
            
            ChatSession saved = chatSessionRepository.save(session);
            log.info("Created new live chat session: {} with token: {}, chatType in DB: {}", 
                saved.getId(), newToken, saved.getChatType());
            return saved;
        } catch (Exception e) {
            log.error("Error creating live chat session", e);
            throw new RuntimeException("Không thể tạo session chat: " + e.getMessage(), e);
        }
    }
    
    @Override
    public LiveChatMessageDTO saveMessage(Long sessionId, String content, ChatMessage.SenderType senderType, User sender) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        // Kiểm tra session có bị đóng không
        if (session.getStatus() == ChatSession.SessionStatus.CLOSED) {
            throw new RuntimeException("Session đã đóng, không thể gửi tin nhắn");
        }
        
        // Nếu session đang ở trạng thái NEW và staff gửi tin nhắn, chuyển sang PROCESSING
        if (session.getStatus() == ChatSession.SessionStatus.NEW 
            && senderType == ChatMessage.SenderType.SUPPORT_AGENT) {
            session.setStatus(ChatSession.SessionStatus.PROCESSING);
            if (session.getStaff() == null && sender != null) {
                session.setStaff(sender);
            }
            log.info("Session {} chuyển sang PROCESSING, staff: {}", sessionId, sender != null ? sender.getEmail() : "unknown");
        }
        
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .messageType(ChatMessage.MessageType.TEXT)
                .senderType(senderType)
                .content(content)
                .sender(sender)
                .seen(false)
                .build();
        
        message = chatMessageRepository.save(message);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
        
        log.info("Saved message {} to session {} (status: {})", message.getId(), session.getId(), session.getStatus());
        
        return convertToDTO(message);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LiveChatMessageDTO> getSessionMessages(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LiveChatSessionDTO> getAllActiveSessions() {
        List<ChatSession> allSessions = chatSessionRepository.findAll();
        log.info("Total sessions in database: {}", allSessions.size());
        
        List<ChatSession> sessions = allSessions.stream()
                .filter(s -> {
                    boolean isLiveChat = s.getChatType() == ChatSession.ChatType.LIVE_CHAT;
                    if (!isLiveChat) {
                        log.debug("Session {} is not LIVE_CHAT, type: {}", s.getId(), s.getChatType());
                    }
                    return isLiveChat;
                })
                .filter(s -> {
                    boolean notClosed = s.getStatus() != ChatSession.SessionStatus.CLOSED;
                    if (!notClosed) {
                        log.debug("Session {} is CLOSED", s.getId());
                    }
                    return notClosed;
                })
                .sorted((a, b) -> {
                    // Sắp xếp: NEW trước, sau đó PROCESSING, sau đó theo updatedAt
                    if (a.getStatus() == ChatSession.SessionStatus.NEW 
                        && b.getStatus() != ChatSession.SessionStatus.NEW) {
                        return -1;
                    }
                    if (a.getStatus() != ChatSession.SessionStatus.NEW 
                        && b.getStatus() == ChatSession.SessionStatus.NEW) {
                        return 1;
                    }
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .collect(Collectors.toList());
        
        log.info("Filtered {} LIVE_CHAT sessions (not closed)", sessions.size());
        
        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LiveChatSessionDTO> getAllSessionsWithFilter(String status, String fromDate, String toDate) {
        List<ChatSession> allSessions = chatSessionRepository.findAll();
        
        log.info("Filtering sessions - status: {}, fromDate: {}, toDate: {}", status, fromDate, toDate);
        
        Stream<ChatSession> stream = allSessions.stream()
                .filter(s -> s.getChatType() == ChatSession.ChatType.LIVE_CHAT);
        
        // Filter by status
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                ChatSession.SessionStatus sessionStatus = ChatSession.SessionStatus.valueOf(status.toUpperCase());
                stream = stream.filter(s -> s.getStatus() == sessionStatus);
                log.info("Filtering by status: {}", sessionStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}", status);
            }
        }
        
        // Filter by date range
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            try {
                java.time.LocalDate from = java.time.LocalDate.parse(fromDate);
                stream = stream.filter(s -> {
                    java.time.LocalDate sessionDate = s.getCreatedAt().toLocalDate();
                    return !sessionDate.isBefore(from);
                });
                log.info("Filtering from date: {}", from);
            } catch (Exception e) {
                log.warn("Invalid fromDate format: {}", fromDate);
            }
        }
        
        if (toDate != null && !toDate.trim().isEmpty()) {
            try {
                java.time.LocalDate to = java.time.LocalDate.parse(toDate);
                stream = stream.filter(s -> {
                    java.time.LocalDate sessionDate = s.getCreatedAt().toLocalDate();
                    return !sessionDate.isAfter(to);
                });
                log.info("Filtering to date: {}", to);
            } catch (Exception e) {
                log.warn("Invalid toDate format: {}", toDate);
            }
        }
        
        // Sort: NEW first, then PROCESSING, then by updatedAt
        List<ChatSession> sessions = stream
                .sorted((a, b) -> {
                    // Sắp xếp: NEW trước, PROCESSING, rồi CLOSED, theo updatedAt
                    if (a.getStatus() == ChatSession.SessionStatus.NEW 
                        && b.getStatus() != ChatSession.SessionStatus.NEW) {
                        return -1;
                    }
                    if (a.getStatus() != ChatSession.SessionStatus.NEW 
                        && b.getStatus() == ChatSession.SessionStatus.NEW) {
                        return 1;
                    }
                    if (a.getStatus() == ChatSession.SessionStatus.PROCESSING 
                        && b.getStatus() == ChatSession.SessionStatus.CLOSED) {
                        return -1;
                    }
                    if (a.getStatus() == ChatSession.SessionStatus.CLOSED 
                        && b.getStatus() == ChatSession.SessionStatus.PROCESSING) {
                        return 1;
                    }
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .collect(Collectors.toList());
        
        log.info("Filtered {} LIVE_CHAT sessions", sessions.size());
        
        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LiveChatSessionDTO> getStaffSessions(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        List<ChatSession> sessions = chatSessionRepository.findAll()
                .stream()
                .filter(s -> s.getChatType() == ChatSession.ChatType.LIVE_CHAT)
                .filter(s -> staff.equals(s.getStaff()))
                .filter(s -> s.getStatus() != ChatSession.SessionStatus.CLOSED)
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .collect(Collectors.toList());
        
        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public ChatSession assignStaffToSession(Long sessionId, Long staffId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        session.setStaff(staff);
        session.setStatus(ChatSession.SessionStatus.PROCESSING);
        session.setUpdatedAt(LocalDateTime.now());
        
        return chatSessionRepository.save(session);
    }
    
    @Override
    public ChatSession closeSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setStatus(ChatSession.SessionStatus.CLOSED);
        session.setEndedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        ChatSession closedSession = chatSessionRepository.save(session);
        
        // Gửi thông báo qua WebSocket cho customer
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SESSION_CLOSED");
            notification.put("sessionToken", session.getSessionToken());
            notification.put("message", "Cuộc trò chuyện đã kết thúc. Cảm ơn bạn đã liên hệ!");
            
            messagingTemplate.convertAndSend("/topic/chat/" + session.getSessionToken(), notification);
            log.info("Sent SESSION_CLOSED notification for session: {}", session.getSessionToken());
        } catch (Exception e) {
            log.error("Error sending SESSION_CLOSED notification", e);
        }
        
        return closedSession;
    }
    
    @Override
    public void markMessagesAsRead(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        // Đánh dấu tất cả tin nhắn không phải của user này là đã đọc
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        for (ChatMessage msg : messages) {
            if (msg.getSender() == null || !msg.getSender().getId().equals(userId)) {
                msg.setSeen(true);
            }
        }
        chatMessageRepository.saveAll(messages);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long countUnreadMessages(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        return messages.stream()
                .filter(msg -> !msg.getSeen())
                .filter(msg -> msg.getSender() == null || !msg.getSender().getId().equals(userId))
                .count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ChatSession> getSessionByToken(String sessionToken) {
        return chatSessionRepository.findBySessionToken(sessionToken)
                .filter(s -> s.getChatType() == ChatSession.ChatType.LIVE_CHAT);
    }
    
    private LiveChatMessageDTO convertToDTO(ChatMessage message) {
        String senderName = "System";
        if (message.getSender() != null) {
            if (message.getSender().getProfile() != null 
                && message.getSender().getProfile().getFullName() != null) {
                senderName = message.getSender().getProfile().getFullName();
            } else {
                senderName = message.getSender().getEmail();
            }
        }
        
        return LiveChatMessageDTO.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .sessionToken(message.getSession().getSessionToken())
                .content(message.getContent())
                .senderType(message.getSenderType().name())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(senderName)
                .isRead(message.getSeen())
                .timestamp(message.getCreatedAt())
                .build();
    }
    
    private LiveChatSessionDTO convertSessionToDTO(ChatSession session) {
        // Lấy tin nhắn cuối
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        ChatMessage lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        
        // Đếm tin nhắn chưa đọc (từ phía staff nếu có staff, ngược lại từ phía customer)
        Long unreadCount = 0L;
        if (session.getStaff() != null) {
            // Đếm tin nhắn từ customer chưa đọc
            unreadCount = messages.stream()
                    .filter(m -> m.getSenderType() == ChatMessage.SenderType.CUSTOMER)
                    .filter(m -> !m.getSeen())
                    .count();
        } else {
            // Đếm tin nhắn từ staff chưa đọc (nếu có)
            unreadCount = messages.stream()
                    .filter(m -> m.getSenderType() == ChatMessage.SenderType.SUPPORT_AGENT)
                    .filter(m -> !m.getSeen())
                    .count();
        }
        
        return LiveChatSessionDTO.builder()
                .id(session.getId())
                .sessionToken(session.getSessionToken())
                .customerId(session.getUser() != null ? session.getUser().getId() : null)
                .customerName(session.getCustomerName())
                .customerEmail(session.getCustomerEmail())
                .staffId(session.getStaff() != null ? session.getStaff().getId() : null)
                .staffName(session.getStaff() != null 
                    ? (session.getStaff().getProfile() != null 
                        ? session.getStaff().getProfile().getFullName() 
                        : session.getStaff().getEmail())
                    : null)
                .status(session.getStatus().name())
                .unreadCount(unreadCount)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : session.getUpdatedAt())
                .lastMessagePreview(lastMessage != null 
                    ? (lastMessage.getContent().length() > 50 
                        ? lastMessage.getContent().substring(0, 50) + "..." 
                        : lastMessage.getContent())
                    : null)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}

