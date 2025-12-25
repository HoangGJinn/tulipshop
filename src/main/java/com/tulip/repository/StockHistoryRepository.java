package com.tulip.repository;

import com.tulip.entity.product.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    
    /**
     * Find all stock history records for a given stock ID, ordered by creation date descending.
     * This supports Requirements 5.1, 5.2, 5.3 - viewing stock history with chronological ordering.
     * 
     * @param stockId the ID of the ProductStock
     * @return list of StockHistory records ordered by createdAt descending (most recent first)
     */
    List<StockHistory> findByStockIdOrderByCreatedAtDesc(Long stockId);
}
