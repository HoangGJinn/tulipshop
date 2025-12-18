package com.tulip.repository;

import com.tulip.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Lấy danh sách đơn hàng của 1 user (để làm trang lịch sử mua hàng sau này)
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Tìm Order theo vnpTxnRef (dùng khi VNPAY callback về)
    Order findByVnpTxnRef(String vnpTxnRef);
}