package com.tulip.repository;

import com.tulip.entity.ChatMessage;
import com.tulip.entity.ChatRoom;
import com.tulip.entity.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Lấy tin nhắn theo phòng chat (phân trang)
    Page<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);
    
    // Lấy tin nhắn chưa đọc của phòng chat
    List<ChatMessage> findByChatRoomAndSeenFalseAndSenderNot(ChatRoom chatRoom, com.tulip.entity.User sender);
    
    // Đếm tin nhắn chưa đọc
    long countByChatRoomAndSeenFalseAndSenderNot(ChatRoom chatRoom, com.tulip.entity.User sender);
    
    // Đánh dấu đã đọc
    @Modifying
    @Query("UPDATE ChatMessage m SET m.seen = true, m.seenAt = :seenAt " +
           "WHERE m.chatRoom = :chatRoom AND m.seen = false AND m.sender != :user")
    int markAsSeen(@Param("chatRoom") ChatRoom chatRoom, 
                   @Param("user") com.tulip.entity.User user,
                   @Param("seenAt") LocalDateTime seenAt);
    
    // Lấy tin nhắn theo loại (không lưu TYPING trong DB)
    List<ChatMessage> findByChatRoomAndTypeIn(ChatRoom chatRoom, List<MessageType> types);
}

