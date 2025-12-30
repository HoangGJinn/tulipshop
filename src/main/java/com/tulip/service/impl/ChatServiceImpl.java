package com.tulip.service.impl;

import com.tulip.dto.ChatMessageDTO;
import com.tulip.dto.ChatRoomDTO;
import com.tulip.dto.ChatTypingDTO;
import com.tulip.entity.ChatMessage;
import com.tulip.entity.ChatRoom;
import com.tulip.entity.User;
import com.tulip.entity.enums.ChatRoomStatus;
import com.tulip.entity.enums.MessageType;
import com.tulip.repository.ChatMessageRepository;
import com.tulip.repository.ChatRoomRepository;
import com.tulip.repository.UserRepository;
import com.tulip.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatRoom createOrGetChatRoom(User customer) {
        // Tìm phòng chat đang mở của khách hàng
        List<ChatRoomStatus> openStatuses = List.of(
            ChatRoomStatus.WAITING, 
            ChatRoomStatus.ASSIGNED
        );
        
        return chatRoomRepository.findByCustomerAndStatusIn(customer, openStatuses)
            .orElseGet(() -> {
                // Tạo phòng chat mới
                ChatRoom newRoom = ChatRoom.builder()
                    .customer(customer)
                    .status(ChatRoomStatus.WAITING)
                    .build();
                
                ChatRoom saved = chatRoomRepository.save(newRoom);
                
                // Log audit
                log.info("ChatRoom created: ID={}, CustomerID={}", saved.getId(), customer.getId());
                
                // Gửi thông báo đến staff về phòng chat mới
                messagingTemplate.convertAndSend("/topic/chat/rooms/waiting", 
                    convertToDTO(saved));
                
                return saved;
            });
    }

    @Override
    @Transactional
    public ChatRoom assignChatRoom(Long chatRoomId, User staff) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        if (room.isAssigned()) {
            throw new RuntimeException("Chat room already assigned");
        }
        
        room.setStaff(staff);
        room.setStatus(ChatRoomStatus.ASSIGNED);
        ChatRoom saved = chatRoomRepository.save(room);
        
        // Log audit
        log.info("ChatRoom assigned: ID={}, StaffID={}", saved.getId(), staff.getId());
        
        // Gửi thông báo đến customer
        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/" + saved.getId() + "/assigned",
            convertToDTO(saved)
        );
        
        // Gửi tin nhắn hệ thống
        sendMessage(chatRoomId, staff, "Nhân viên đã nhận cuộc trò chuyện", MessageType.SYSTEM);
        
        return saved;
    }

    @Override
    @Transactional
    public ChatRoom closeChatRoom(Long chatRoomId, User user) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        // Chỉ customer hoặc staff của phòng mới được đóng
        if (!room.getCustomer().getId().equals(user.getId()) && 
            (room.getStaff() == null || !room.getStaff().getId().equals(user.getId()))) {
            throw new RuntimeException("Unauthorized to close this chat room");
        }
        
        room.setStatus(ChatRoomStatus.CLOSED);
        ChatRoom saved = chatRoomRepository.save(room);
        
        // Log audit
        log.info("ChatRoom closed: ID={}, UserID={}", saved.getId(), user.getId());
        
        // Gửi thông báo
        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/" + saved.getId() + "/closed",
            convertToDTO(saved)
        );
        
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomDTO getChatRoomDTO(Long chatRoomId, User user) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        // Kiểm tra quyền truy cập
        if (!room.getCustomer().getId().equals(user.getId()) && 
            (room.getStaff() == null || !room.getStaff().getId().equals(user.getId()))) {
            throw new RuntimeException("Unauthorized to access this chat room");
        }
        
        return convertToDTO(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getCustomerChatRooms(User customer) {
        return chatRoomRepository.findByCustomerOrderByLastMessageAtDesc(customer)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getStaffChatRooms(User staff) {
        return chatRoomRepository.findByStaffAndStatus(staff, ChatRoomStatus.ASSIGNED)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getWaitingChatRooms() {
        return chatRoomRepository.findWaitingRoomsOrderedByPriority(ChatRoomStatus.WAITING)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageDTO sendMessage(Long chatRoomId, User sender, String content, MessageType type) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        // Kiểm tra quyền gửi tin nhắn
        if (!room.getCustomer().getId().equals(sender.getId()) && 
            (room.getStaff() == null || !room.getStaff().getId().equals(sender.getId()))) {
            throw new RuntimeException("Unauthorized to send message in this chat room");
        }
        
        // Chỉ lưu TEXT và SYSTEM vào DB, không lưu TYPING
        if (type == MessageType.TYPING) {
            // Chỉ gửi real-time, không lưu DB
            ChatTypingDTO typingDTO = ChatTypingDTO.builder()
                .chatRoomId(chatRoomId)
                .userId(sender.getId())
                .userName(getUserName(sender))
                .isTyping(true)
                .build();
            
            messagingTemplate.convertAndSend(
                "/topic/chat/rooms/" + chatRoomId + "/typing",
                typingDTO
            );
            
            return null; // Không lưu vào DB
        }
        
        // Tạo và lưu tin nhắn
        ChatMessage message = ChatMessage.builder()
            .chatRoom(room)
            .sender(sender)
            .type(type)
            .content(content)
            .seen(false)
            .build();
        
        ChatMessage saved = chatMessageRepository.save(message);
        
        // Cập nhật lastMessageAt của phòng
        room.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(room);
        
        // Convert to DTO
        ChatMessageDTO messageDTO = convertToDTO(saved);
        
        // Gửi qua WebSocket đến phòng chat
        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/" + chatRoomId + "/messages",
            messageDTO
        );
        
        // Gửi đến user cụ thể nếu cần
        if (room.getStaff() != null && room.getStaff().getId().equals(sender.getId())) {
            messagingTemplate.convertAndSendToUser(
                room.getCustomer().getId().toString(),
                "/queue/chat/messages",
                messageDTO
            );
        } else if (room.getCustomer().getId().equals(sender.getId()) && room.getStaff() != null) {
            messagingTemplate.convertAndSendToUser(
                room.getStaff().getId().toString(),
                "/queue/chat/messages",
                messageDTO
            );
        }
        
        return messageDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDTO> getChatHistory(Long chatRoomId, User user, Pageable pageable) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        // Kiểm tra quyền truy cập
        if (!room.getCustomer().getId().equals(user.getId()) && 
            (room.getStaff() == null || !room.getStaff().getId().equals(user.getId()))) {
            throw new RuntimeException("Unauthorized to access this chat room");
        }
        
        // Lấy lịch sử tin nhắn (chỉ TEXT và SYSTEM, không có TYPING)
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(
            room, pageable
        );
        
        return messages.map(this::convertToDTO);
    }

    @Override
    @Transactional
    public void markMessagesAsSeen(Long chatRoomId, User user) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        // Đánh dấu đã đọc
        chatMessageRepository.markAsSeen(room, user, LocalDateTime.now());
        
        // Cập nhật lastSeenAt
        if (room.getCustomer().getId().equals(user.getId())) {
            room.setCustomerLastSeenAt(LocalDateTime.now());
        } else if (room.getStaff() != null && room.getStaff().getId().equals(user.getId())) {
            room.setStaffLastSeenAt(LocalDateTime.now());
        }
        chatRoomRepository.save(room);
        
        // Gửi thông báo SEEN qua WebSocket (không lưu DB)
        ChatMessageDTO seenDTO = ChatMessageDTO.builder()
            .chatRoomId(chatRoomId)
            .senderId(user.getId())
            .type(MessageType.SEEN)
            .createdAt(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/" + chatRoomId + "/seen",
            seenDTO
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long chatRoomId, User user) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        return chatMessageRepository.countByChatRoomAndSeenFalseAndSenderNot(room, user);
    }

    @Override
    public void sendTypingIndicator(Long chatRoomId, User user, boolean isTyping) {
        // Không lưu DB, chỉ gửi real-time
        ChatTypingDTO typingDTO = ChatTypingDTO.builder()
            .chatRoomId(chatRoomId)
            .userId(user.getId())
            .userName(getUserName(user))
            .isTyping(isTyping)
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/" + chatRoomId + "/typing",
            typingDTO
        );
    }

    // Helper methods
    private ChatRoomDTO convertToDTO(ChatRoom room) {
        long unreadCount = 0;
        if (room.getCustomer() != null) {
            unreadCount = getUnreadCount(room.getId(), room.getCustomer());
        }
        
        return ChatRoomDTO.builder()
            .id(room.getId())
            .customerId(room.getCustomer().getId())
            .customerName(getUserName(room.getCustomer()))
            .customerAvatar(getUserAvatar(room.getCustomer()))
            .staffId(room.getStaff() != null ? room.getStaff().getId() : null)
            .staffName(room.getStaff() != null ? getUserName(room.getStaff()) : null)
            .staffAvatar(room.getStaff() != null ? getUserAvatar(room.getStaff()) : null)
            .status(room.getStatus())
            .lastMessageAt(room.getLastMessageAt())
            .unreadCount(unreadCount)
            .createdAt(room.getCreatedAt())
            .build();
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
            .id(message.getId())
            .chatRoomId(message.getChatRoom().getId())
            .senderId(message.getSender().getId())
            .senderName(getUserName(message.getSender()))
            .senderAvatar(getUserAvatar(message.getSender()))
            .type(message.getType())
            .content(message.getContent())
            .seen(message.getSeen())
            .seenAt(message.getSeenAt())
            .createdAt(message.getCreatedAt())
            .build();
    }

    private String getUserName(User user) {
        return user.getProfile() != null && user.getProfile().getFullName() != null
            ? user.getProfile().getFullName()
            : user.getEmail();
    }

    private String getUserAvatar(User user) {
        return user.getProfile() != null ? user.getProfile().getAvatar() : null;
    }
}

