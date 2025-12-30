package com.tulip.entity;

import com.tulip.entity.enums.ChatRoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"customer", "staff", "messages"})
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = true)
    private User staff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChatRoomStatus status = ChatRoomStatus.WAITING;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "customer_last_seen_at")
    private LocalDateTime customerLastSeenAt;

    @Column(name = "staff_last_seen_at")
    private LocalDateTime staffLastSeenAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isAssigned() {
        return status == ChatRoomStatus.ASSIGNED && staff != null;
    }

    public boolean isWaiting() {
        return status == ChatRoomStatus.WAITING;
    }

    public boolean isClosed() {
        return status == ChatRoomStatus.CLOSED;
    }
}

