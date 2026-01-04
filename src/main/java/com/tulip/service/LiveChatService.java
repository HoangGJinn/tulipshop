package com.tulip.service;

import com.tulip.dto.LiveChatMessageDTO;
import com.tulip.dto.LiveChatSessionDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;

import java.util.List;
import java.util.Optional;

/**
 * Service cho tính năng Live Chat CSKH
 */
public interface LiveChatService {
    
    /**
     * Tạo hoặc lấy session cho user đã đăng nhập
     * @param user User đã đăng nhập (required)
     * @param sessionToken Token của session cũ (nếu có)
     * @return ChatSession
     */
    ChatSession getOrCreateSession(User user, String sessionToken);
    
    /**
     * Lưu tin nhắn vào database
     */
    LiveChatMessageDTO saveMessage(Long sessionId, String content, ChatMessage.SenderType senderType, User sender);
    
    /**
     * Lấy danh sách tin nhắn của một session
     */
    List<LiveChatMessageDTO> getSessionMessages(Long sessionId);
    
    /**
     * Lấy danh sách session cho admin (tất cả sessions đang chờ hoặc đang xử lý)
     */
    List<LiveChatSessionDTO> getAllActiveSessions();
    
    /**
     * Lấy tất cả sessions với filter
     * @param status - NEW, PROCESSING, CLOSED, hoặc null (all)
     * @param fromDate - yyyy-MM-dd
     * @param toDate - yyyy-MM-dd
     */
    List<LiveChatSessionDTO> getAllSessionsWithFilter(String status, String fromDate, String toDate);
    
    /**
     * Lấy danh sách session của một staff
     */
    List<LiveChatSessionDTO> getStaffSessions(Long staffId);
    
    /**
     * Staff nhận xử lý một session
     */
    ChatSession assignStaffToSession(Long sessionId, Long staffId);
    
    /**
     * Đóng một session
     */
    ChatSession closeSession(Long sessionId);
    
    /**
     * Đánh dấu tin nhắn đã đọc
     */
    void markMessagesAsRead(Long sessionId, Long userId);
    
    /**
     * Đếm số tin nhắn chưa đọc của một session
     */
    Long countUnreadMessages(Long sessionId, Long userId);
    
    /**
     * Lấy session theo token
     */
    Optional<ChatSession> getSessionByToken(String sessionToken);
}

