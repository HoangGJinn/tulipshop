package com.tulip.entity.chat;

import com.tulip.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // Nullable để hỗ trợ khách vãng lai
    private User user;

    @Column(name = "session_token", unique = true, nullable = false)
    private String sessionToken;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = true) // Staff đang xử lý chat này
    private User staff;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type", nullable = false)
    @Builder.Default
    private ChatType chatType = ChatType.AI_CHAT; // Phân biệt AI chat và Live chat

    @Column(name = "customer_context", columnDefinition = "TEXT")
    private String customerContext; // Store customer preferences, history, etc.

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public enum SessionStatus {
        ACTIVE, ENDED, ARCHIVED, // Cho AI chat
        NEW, PROCESSING, CLOSED   // Cho Live chat
    }
    
    public enum ChatType {
        AI_CHAT,      // Chat với AI bot
        LIVE_CHAT     // Chat với nhân viên
    }
}
