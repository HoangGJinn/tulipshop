package com.tulip.repository;

import com.tulip.entity.ShippingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingOrderRepository extends JpaRepository<ShippingOrder, Long> {
    Optional<ShippingOrder> findByOrder_Id(Long orderId);
    Optional<ShippingOrder> findByOrder_OrderCode(String orderCode);
}
