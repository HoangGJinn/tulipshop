package com.tulip.repository;

import com.tulip.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.stock.id = :stockId " +
           "AND oi.order.status IN ('PENDING', 'CONFIRMED')")
    Integer calculateReservedStock(@Param("stockId") Long stockId);
}