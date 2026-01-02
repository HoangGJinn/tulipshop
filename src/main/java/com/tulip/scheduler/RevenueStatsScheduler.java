package com.tulip.scheduler;

import com.tulip.service.RevenueStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job để tính toán và lưu revenue stats tự động
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RevenueStatsScheduler {
    
    private final RevenueStatsService revenueStatsService;
    
    /**
     * Chạy mỗi ngày lúc 00:05 (5 phút sau nửa đêm)
     * Tính stats cho ngày hôm qua
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void calculateDailyStats() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("Starting daily stats calculation for {}", yesterday);
            revenueStatsService.calculateAndSaveDailyStats(yesterday);
            log.info("Daily stats calculation completed for {}", yesterday);
        } catch (Exception e) {
            log.error("Error calculating daily stats", e);
        }
    }
    
    /**
     * Chạy mỗi Chủ nhật lúc 01:00
     * Tính stats cho tuần vừa qua
     */
    @Scheduled(cron = "0 0 1 ? * SUN")
    public void calculateWeeklyStats() {
        try {
            LocalDate lastSunday = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue());
            log.info("Starting weekly stats calculation ending {}", lastSunday);
            revenueStatsService.calculateAndSaveWeeklyStats(lastSunday);
            log.info("Weekly stats calculation completed");
        } catch (Exception e) {
            log.error("Error calculating weekly stats", e);
        }
    }
    
    /**
     * Chạy vào ngày đầu tiên của mỗi tháng lúc 02:00
     * Tính stats cho tháng trước
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void calculateMonthlyStats() {
        try {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            log.info("Starting monthly stats calculation for {}/{}", lastMonth.getMonthValue(), lastMonth.getYear());
            revenueStatsService.calculateAndSaveMonthlyStats(lastMonth);
            log.info("Monthly stats calculation completed");
        } catch (Exception e) {
            log.error("Error calculating monthly stats", e);
        }
    }
    
    /**
     * Chạy vào ngày đầu tiên của quý (1/1, 1/4, 1/7, 1/10) lúc 03:00
     * Tính stats cho quý trước
     */
    @Scheduled(cron = "0 0 3 1 1,4,7,10 ?")
    public void calculateQuarterlyStats() {
        try {
            LocalDate lastQuarter = LocalDate.now().minusMonths(3);
            log.info("Starting quarterly stats calculation for Q{} {}", 
                    (lastQuarter.getMonthValue() - 1) / 3 + 1, lastQuarter.getYear());
            revenueStatsService.calculateAndSaveQuarterlyStats(lastQuarter);
            log.info("Quarterly stats calculation completed");
        } catch (Exception e) {
            log.error("Error calculating quarterly stats", e);
        }
    }
    
    /**
     * Tính stats cho ngày hôm nay (chạy mỗi giờ để update real-time)
     * Dùng cho dashboard hiển thị dữ liệu hôm nay
     */
    @Scheduled(cron = "0 0 * * * ?") // Mỗi giờ
    public void updateTodayStats() {
        try {
            LocalDate today = LocalDate.now();
            revenueStatsService.calculateAndSaveDailyStats(today);
            log.debug("Updated today's stats");
        } catch (Exception e) {
            log.error("Error updating today's stats", e);
        }
    }
}
