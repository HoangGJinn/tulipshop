package com.tulip.repository;

import com.tulip.entity.Notification;
import com.tulip.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Lấy danh sách thông báo của user, bao gồm cả thông báo chung (user = NULL)
     * Sắp xếp theo thời gian mới nhất
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId OR n.user IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findByUserOrPublic(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Lấy danh sách thông báo theo loại, bao gồm cả thông báo chung
     */
    @Query("SELECT n FROM Notification n WHERE (n.user.id = :userId OR n.user IS NULL) AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByUserOrPublicAndType(
        @Param("userId") Long userId,
        @Param("type") Notification.NotificationType type,
        Pageable pageable
    );
    
    /**
     * Đếm số thông báo chưa đọc của user (bao gồm cả thông báo chung)
     * Sử dụng LEFT JOIN với NotificationRead để kiểm tra trạng thái đã đọc
     */
    @Query("SELECT COUNT(n) FROM Notification n " +
           "WHERE (n.user.id = :userId OR n.user IS NULL) " +
           "AND NOT EXISTS (SELECT 1 FROM NotificationRead nr WHERE nr.notification = n AND nr.user.id = :userId)")
    Long countUnreadByUserOrPublic(@Param("userId") Long userId);
    
    /**
     * Đếm số thông báo chưa đọc theo loại (bao gồm cả thông báo chung)
     */
    @Query("SELECT COUNT(n) FROM Notification n " +
           "WHERE (n.user.id = :userId OR n.user IS NULL) AND n.type = :type " +
           "AND NOT EXISTS (SELECT 1 FROM NotificationRead nr WHERE nr.notification = n AND nr.user.id = :userId)")
    Long countUnreadByUserOrPublicAndType(@Param("userId") Long userId, @Param("type") Notification.NotificationType type);
    
    /**
     * Xóa thông báo cũ hơn số ngày chỉ định
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    /**
     * Lấy danh sách thông báo theo các loại chỉ định (cho Admin)
     */
    Page<Notification> findByTypeIn(List<Notification.NotificationType> types, Pageable pageable);
}
