package com.tulip.entity.chat;

import com.tulip.entity.User;
import com.tulip.entity.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore // [QUAN TRỌNG] Chặn vòng lặp vô tận khiến JSON bị lỗi }}}}}
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType; // Sẽ hết đỏ khi có enum bên dưới

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @ManyToMany
    @JoinTable(
            name = "message_recommended_products",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private List<Product> recommendedProducts = new ArrayList<>();

    @Column(name = "policy_advice", columnDefinition = "TEXT")
    private String policyAdvice;

    @Builder.Default
    private Boolean seen = false;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = true)
    private User sender;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // --- ĐỊNH NGHĨA ENUM BÊN TRONG CLASS ĐỂ HẾT LỖI ĐỎ BIÊN DỊCH ---
    public enum MessageType {
        TEXT, PRODUCT_RECOMMENDATION, POLICY_ADVICE, ORDER_INQUIRY, GENERAL_INQUIRY
    }

    public enum SenderType {
        CUSTOMER, AI_BOT, SUPPORT_AGENT
    }
}