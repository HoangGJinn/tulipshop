package com.tulip.repository;

import com.tulip.entity.ChatRoom;
import com.tulip.entity.User;
import com.tulip.entity.enums.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    // Tìm phòng chat của khách hàng (mở hoặc đã gán)
    Optional<ChatRoom> findByCustomerAndStatusIn(User customer, List<ChatRoomStatus> statuses);
    
    // Tìm phòng chat đang chờ
    List<ChatRoom> findByStatusOrderByCreatedAtAsc(ChatRoomStatus status);
    
    // Tìm phòng chat của nhân viên
    List<ChatRoom> findByStaffAndStatus(User staff, ChatRoomStatus status);
    
    // Tìm phòng chat của khách hàng (tất cả trạng thái)
    List<ChatRoom> findByCustomerOrderByLastMessageAtDesc(User customer);
    
    // Đếm số phòng đang chờ
    long countByStatus(ChatRoomStatus status);
    
    // Tìm phòng chat theo ID và customer
    Optional<ChatRoom> findByIdAndCustomer(Long id, User customer);
    
    // Tìm phòng chat theo ID và staff
    Optional<ChatRoom> findByIdAndStaff(Long id, User staff);
    
    // Lấy danh sách phòng chat ưu tiên (VIP customers, pending orders, wait time)
    // TODO: Thêm logic ưu tiên VIP và pending orders khi có dữ liệu
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.status = :status " +
           "ORDER BY cr.createdAt ASC")
    List<ChatRoom> findWaitingRoomsOrderedByPriority(@Param("status") ChatRoomStatus status);
}

