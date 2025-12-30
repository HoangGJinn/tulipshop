package com.tulip.service;

import com.tulip.dto.ChatMessageDTO;
import com.tulip.dto.ChatRoomDTO;
import com.tulip.entity.ChatRoom;
import com.tulip.entity.User;
import com.tulip.entity.enums.ChatRoomStatus;
import com.tulip.entity.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    
    // ChatRoom management
    ChatRoom createOrGetChatRoom(User customer);
    ChatRoom assignChatRoom(Long chatRoomId, User staff);
    ChatRoom closeChatRoom(Long chatRoomId, User user);
    ChatRoomDTO getChatRoomDTO(Long chatRoomId, User user);
    List<ChatRoomDTO> getCustomerChatRooms(User customer);
    List<ChatRoomDTO> getStaffChatRooms(User staff);
    List<ChatRoomDTO> getWaitingChatRooms();
    
    // Message management
    ChatMessageDTO sendMessage(Long chatRoomId, User sender, String content, MessageType type);
    Page<ChatMessageDTO> getChatHistory(Long chatRoomId, User user, Pageable pageable);
    void markMessagesAsSeen(Long chatRoomId, User user);
    long getUnreadCount(Long chatRoomId, User user);
    
    // Typing indicator (không lưu DB)
    void sendTypingIndicator(Long chatRoomId, User user, boolean isTyping);
}

