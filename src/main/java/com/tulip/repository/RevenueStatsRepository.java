package com.tulip.repository;

import com.tulip.entity.RevenueStats;
import com.tulip.entity.RevenueStats.StatsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueStatsRepository extends JpaRepository<RevenueStats, Long> {
    // Tìm stats theo ngày và loại
    Optional<RevenueStats> findByStatsDateAndStatsType(LocalDate statsDate, StatsType statsType);
    
    // Tìm stats theo loại trong khoảng thời gian
    List<RevenueStats> findByStatsTypeAndStatsDateBetweenOrderByStatsDateAsc(
            StatsType statsType, 
            LocalDate startDate, 
            LocalDate endDate
    );
    
    // Lấy bản ghi mới nhất theo loại stats
    Optional<RevenueStats> findFirstByStatsTypeOrderByStatsDateDesc(StatsType statsType);
    
    // Lấy 7 ngày gần nhất
    @Query("SELECT rs FROM RevenueStats rs " +
           "WHERE rs.statsType = 'DAILY' " +
           "AND rs.statsDate >= :startDate " +
           "AND rs.statsDate <= :endDate " +
           "ORDER BY rs.statsDate ASC")
    List<RevenueStats> findLast7Days(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Lấy 7 ngày gần nhất
    @Query("SELECT rs FROM RevenueStats rs " +
           "WHERE rs.statsType = 'DAILY' " +
           "AND rs.statsDate >= :startDate " +
           "AND rs.statsDate <= :endDate " +
           "ORDER BY rs.statsDate ASC")
    List<RevenueStats> findLast30Days(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Lấy dữ liệu DAILY của tháng hiện tại (để gom thành tuần)
    @Query("SELECT rs FROM RevenueStats rs " +
           "WHERE rs.statsType = 'DAILY' " +
           "AND rs.statsDate >= :startDate " +
           "AND rs.statsDate <= :endDate " +
           "ORDER BY rs.statsDate ASC")
    List<RevenueStats> findCurrentMonthDays(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Lấy dữ liệu MONTHLY của 3 tháng gần nhất
    @Query("SELECT rs FROM RevenueStats rs " +
           "WHERE rs.statsType = 'DAILY' " +
           "AND rs.statsDate >= :startDate " +
           "ORDER BY rs.statsDate ASC")
    List<RevenueStats> findLast3Months(@Param("startDate") LocalDate startDate);
    
    // Kiểm tra tồn tại stats theo ngày và loại
    boolean existsByStatsDateAndStatsType(LocalDate statsDate, StatsType statsType);
}
