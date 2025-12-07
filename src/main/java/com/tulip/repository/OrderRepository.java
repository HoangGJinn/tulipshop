package com.tulip.repository;

import com.tulip.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Lấy danh sách đơn hàng của 1 user (để hiển thị lịch sử mua hàng)
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}