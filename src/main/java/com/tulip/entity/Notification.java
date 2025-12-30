package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_created", columnList = "user_id,created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Người dùng nhận thông báo
     * NULL = thông báo chung cho tất cả người dùng (broadcast)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
    
    /**
     * Tiêu đề thông báo (ngắn gọn)
     */
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * Nội dung chi tiết thông báo
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * Link để điều hướng khi click vào thông báo
     * VD: /orders/123, /products/456
     */
    @Column(name = "target_url", length = 500)
    private String link;
    
    /**
     * URL ảnh minh họa thông báo (Cloudinary)
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    /**
     * Loại thông báo
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;
    
    /**
     * Thời gian tạo thông báo
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Enum định nghĩa các loại thông báo
     */
    public enum NotificationType {
        ORDER,      // Thông báo về đơn hàng
        PROMOTION,  // Thông báo khuyến mãi
        SYSTEM      // Thông báo hệ thống
    }
}
