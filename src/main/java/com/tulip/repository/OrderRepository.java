package com.tulip.repository;

import com.tulip.entity.Order;
import com.tulip.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.variant v " +
           "LEFT JOIN FETCH oi.product p " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    Order findByVnpTxnRef(String vnpTxnRef);
    
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.variant v " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH oi.size " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
    
    @Query("SELECT o FROM Order o " +
           "WHERE o.status = :status " +
           "AND o.paymentExpireAt IS NOT NULL " +
           "AND o.paymentExpireAt < :now")
    List<Order> findExpiredPendingOrders(@Param("status") OrderStatus status, 
                                         @Param("now") LocalDateTime now);
}