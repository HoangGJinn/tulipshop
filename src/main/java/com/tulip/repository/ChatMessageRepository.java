package com.tulip.repository;

import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    
    List<ChatMessage> findBySessionAndSeenOrderByCreatedAtAsc(ChatSession session, Boolean seen);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.session = :session AND cm.seen = false")
    Long countUnreadMessages(@Param("session") ChatSession session);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.session = :session AND cm.senderType = :senderType ORDER BY cm.createdAt DESC")
    List<ChatMessage> findBySessionAndSenderTypeOrderByCreatedAtDesc(@Param("session") ChatSession session, 
                                                                     @Param("senderType") ChatMessage.SenderType senderType);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.createdAt < :cutoffDate")
    List<ChatMessage> findOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);
}
