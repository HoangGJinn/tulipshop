package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "notification_reads", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_id"}),
    indexes = {
        @Index(name = "idx_user_notification", columnList = "user_id,notification_id"),
        @Index(name = "idx_notification", columnList = "notification_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRead {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User đã đọc thông báo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
    
    /**
     * Thông báo đã được đọc
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Notification notification;
    
    /**
     * Thời điểm đọc
     */
    @CreationTimestamp
    @Column(name = "read_at", nullable = false, updatable = false)
    private LocalDateTime readAt;
}
