package com.tulip.repository;

import com.tulip.entity.Notification;
import com.tulip.entity.NotificationRead;
import com.tulip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    
    /**
     * Kiểm tra user đã đọc thông báo chưa
     */
    boolean existsByUserAndNotification(User user, Notification notification);
    
    /**
     * Tìm bản ghi đã đọc
     */
    Optional<NotificationRead> findByUserAndNotification(User user, Notification notification);
    
    /**
     * Lấy danh sách ID thông báo đã đọc của user
     */
    @Query("SELECT nr.notification.id FROM NotificationRead nr WHERE nr.user.id = :userId")
    List<Long> findReadNotificationIdsByUserId(@Param("userId") Long userId);
    
    /**
     * Đếm số thông báo đã đọc của user
     */
    long countByUser(User user);
    
    /**
     * Xóa tất cả bản ghi đã đọc của một thông báo (khi xóa thông báo)
     */
    void deleteByNotification(Notification notification);
}
