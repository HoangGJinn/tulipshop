package com.tulip.repository;

import com.tulip.entity.chat.ChatSession;
import com.tulip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    Optional<ChatSession> findBySessionToken(String sessionToken);
    
    List<ChatSession> findByUserOrderByCreatedAtDesc(User user);
    
    List<ChatSession> findByStatusOrderByCreatedAtDesc(ChatSession.SessionStatus status);
    
    @Query("SELECT cs FROM ChatSession cs WHERE cs.status = :status AND cs.createdAt < :cutoffDate")
    List<ChatSession> findByStatusAndCreatedAtBefore(@Param("status") ChatSession.SessionStatus status, 
                                                   @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.user = :user AND cs.status = :status")
    Long countByUserAndStatus(@Param("user") User user, @Param("status") ChatSession.SessionStatus status);
}
