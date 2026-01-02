package com.tulip.service.impl;

import com.tulip.dto.NotificationDTO;
import com.tulip.dto.NotificationRequest;
import com.tulip.entity.Notification;
import com.tulip.entity.NotificationRead;
import com.tulip.entity.User;
import com.tulip.repository.NotificationReadRepository;
import com.tulip.repository.NotificationRepository;
import com.tulip.repository.UserRepository;
import com.tulip.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation của NotificationService
 * Logic mới: Sử dụng NotificationRead để track trạng thái đã đọc
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Lấy user hiện tại từ SecurityContext
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }
    
    @Override
    @Transactional
    public NotificationDTO sendNotification(String username, NotificationRequest request) {
        try {
            // Tìm user theo email
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
            
            // Tạo notification entity
            Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .link(request.getLink())
                .imageUrl(request.getImageUrl())
                .type(request.getType())
                .build();
            
            // Lưu vào database
            notification = notificationRepository.save(notification);
            log.info("✅ Đã lưu thông báo vào database: ID={}, User={}, Title={}", 
                notification.getId(), username, notification.getTitle());
            
            // Chuyển đổi sang DTO (chưa đọc)
            NotificationDTO dto = NotificationDTO.fromEntity(notification, false);
            
            // Gửi thông báo qua WebSocket đến user cụ thể
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                dto
            );
            log.info("📡 Đã gửi thông báo qua WebSocket đến user: {}", username);
            
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo đến user {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi thông báo", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(Pageable pageable) {
        User user = getCurrentUser();
        Page<Notification> notifications = notificationRepository.findByUserOrPublic(user.getId(), pageable);
        
        // Lấy danh sách ID thông báo đã đọc
        List<Long> readIds = notificationReadRepository.findReadNotificationIdsByUserId(user.getId());
        Set<Long> readIdSet = readIds.stream().collect(Collectors.toSet());
        
        // Map sang DTO với trạng thái isRead
        return notifications.map(n -> NotificationDTO.fromEntity(n, readIdSet.contains(n.getId())));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsByType(Notification.NotificationType type, Pageable pageable) {
        User user = getCurrentUser();
        Page<Notification> notifications = notificationRepository.findByUserOrPublicAndType(user.getId(), type, pageable);
        
        // Lấy danh sách ID thông báo đã đọc
        List<Long> readIds = notificationReadRepository.findReadNotificationIdsByUserId(user.getId());
        Set<Long> readIdSet = readIds.stream().collect(Collectors.toSet());
        
        return notifications.map(n -> NotificationDTO.fromEntity(n, readIdSet.contains(n.getId())));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long countUnreadNotifications() {
        User user = getCurrentUser();
        return notificationRepository.countUnreadByUserOrPublic(user.getId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long countUnreadNotificationsByType(Notification.NotificationType type) {
        User user = getCurrentUser();
        return notificationRepository.countUnreadByUserOrPublicAndType(user.getId(), type);
    }
    
    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));
        
        // Kiểm tra quyền: Chỉ cho phép đọc thông báo của mình hoặc thông báo chung
        if (notification.getUser() != null && !notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không có quyền truy cập thông báo này");
        }
        
        // Kiểm tra đã đọc chưa
        if (!notificationReadRepository.existsByUserAndNotification(user, notification)) {
            // Tạo bản ghi đã đọc
            NotificationRead notificationRead = NotificationRead.builder()
                .user(user)
                .notification(notification)
                .build();
            notificationReadRepository.save(notificationRead);
            log.info("✅ User {} đã đọc thông báo {}", user.getEmail(), notificationId);
        }
    }
    
    @Override
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        
        // Lấy tất cả thông báo của user (bao gồm broadcast)
        Page<Notification> notifications = notificationRepository.findByUserOrPublic(
            user.getId(), 
            Pageable.unpaged()
        );
        
        // Lấy danh sách đã đọc
        List<Long> readIds = notificationReadRepository.findReadNotificationIdsByUserId(user.getId());
        Set<Long> readIdSet = readIds.stream().collect(Collectors.toSet());
        
        // Tạo bản ghi cho các thông báo chưa đọc
        int count = 0;
        for (Notification notification : notifications) {
            if (!readIdSet.contains(notification.getId())) {
                NotificationRead notificationRead = NotificationRead.builder()
                    .user(user)
                    .notification(notification)
                    .build();
                notificationReadRepository.save(notificationRead);
                count++;
            }
        }
        
        log.info("✅ Đã đánh dấu {} thông báo là đã đọc cho user {}", count, user.getEmail());
    }
    
    @Override
    @Transactional
    public void markAllAsReadByType(Notification.NotificationType type) {
        User user = getCurrentUser();
        
        // Lấy tất cả thông báo theo loại
        Page<Notification> notifications = notificationRepository.findByUserOrPublicAndType(
            user.getId(),
            type,
            Pageable.unpaged()
        );
        
        // Lấy danh sách đã đọc
        List<Long> readIds = notificationReadRepository.findReadNotificationIdsByUserId(user.getId());
        Set<Long> readIdSet = readIds.stream().collect(Collectors.toSet());
        
        // Tạo bản ghi cho các thông báo chưa đọc
        int count = 0;
        for (Notification notification : notifications) {
            if (!readIdSet.contains(notification.getId())) {
                NotificationRead notificationRead = NotificationRead.builder()
                    .user(user)
                    .notification(notification)
                    .build();
                notificationReadRepository.save(notificationRead);
                count++;
            }
        }
        
        log.info("✅ Đã đánh dấu {} thông báo loại {} là đã đọc", count, type);
    }
    
    @Override
    @Transactional
    public void deleteOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int count = notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Đã xóa {} thông báo cũ hơn {} ngày", count, daysToKeep);
    }
    
    @Override
    @Transactional
    public NotificationDTO sendBroadcastNotification(NotificationRequest request) {
        try {
            // Tạo notification entity với user = NULL (broadcast)
            Notification notification = Notification.builder()
                .user(null) // NULL = thông báo chung
                .title(request.getTitle())
                .content(request.getContent())
                .link(request.getLink())
                .imageUrl(request.getImageUrl())
                .type(request.getType())
                .build();
            
            // Lưu vào database (chỉ 1 bản ghi)
            notification = notificationRepository.save(notification);
            log.info("✅ Đã lưu thông báo broadcast vào database: ID={}, Title={}", 
                notification.getId(), notification.getTitle());
            
            // Chuyển đổi sang DTO (chưa đọc)
            NotificationDTO dto = NotificationDTO.fromEntity(notification, false);
            
            // Gửi thông báo qua WebSocket đến topic public
            messagingTemplate.convertAndSend("/topic/public-notifications", dto);
            log.info("📡 Đã gửi thông báo broadcast qua WebSocket đến /topic/public-notifications");
            
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo broadcast: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi thông báo broadcast", e);
        }
    }
}
