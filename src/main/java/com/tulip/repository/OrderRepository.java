package com.tulip.repository;

import com.tulip.entity.Order;
import com.tulip.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    Order findByOrderCode(String orderCode);
    
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

    // Admin: Lấy tất cả đơn hàng (mới nhất trước)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.variant v " +
           "LEFT JOIN FETCH oi.product p " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllWithDetails();

    // Admin: Lấy đơn hàng theo trạng thái
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "WHERE o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByStatus(@Param("status") OrderStatus status);

    // Admin: Lấy đơn hàng theo user
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithDetails(@Param("userId") Long userId);

    // Admin: Lấy đơn hàng theo khoảng thời gian
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // User: Lấy đơn hàng theo user với phân trang
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.variant v " +
           "LEFT JOIN FETCH oi.product p " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdPaginated(@Param("userId") Long userId, Pageable pageable);

    // User: Lấy đơn hàng theo user và status với phân trang
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.variant v " +
           "LEFT JOIN FETCH oi.product p " +
           "WHERE o.user.id = :userId AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdAndStatusPaginated(@Param("userId") Long userId,
                                                @Param("status") OrderStatus status,
                                                Pageable pageable);
    
    /**
     * Lấy top sản phẩm bán chạy nhất
     * Trả về: productId, productName, thumbnail, price, totalSold, totalRevenue
     */
    @Query("SELECT p.id, p.name, p.thumbnail, p.basePrice, " +
           "SUM(oi.quantity) as totalSold, " +
           "SUM(oi.quantity * oi.priceAtPurchase) as totalRevenue " +
           "FROM OrderItem oi " +
           "JOIN oi.product p " +
           "JOIN oi.order o " +
           "WHERE o.status = 'DELIVERED' " +
           "GROUP BY p.id, p.name, p.thumbnail, p.basePrice " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
}