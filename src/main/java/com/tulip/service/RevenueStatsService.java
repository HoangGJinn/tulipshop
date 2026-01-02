package com.tulip.service;

import com.tulip.entity.RevenueStats;

import java.time.LocalDate;
import java.util.List;

public interface RevenueStatsService {
    
    // Tính toán và lưu stats cho ngày
    RevenueStats calculateAndSaveDailyStats(LocalDate date);
    
    // Tính toán và lưu stats cho tuần
    RevenueStats calculateAndSaveWeeklyStats(LocalDate endDate);
    
    // Tính toán và lưu stats cho tháng
    RevenueStats calculateAndSaveMonthlyStats(LocalDate date);
    
    // Tính toán và lưu stats cho quý
    RevenueStats calculateAndSaveQuarterlyStats(LocalDate endDate);
    
    // Lấy stats theo ngày
    RevenueStats getDailyStats(LocalDate date);
    
    // Lấy 7 ngày gần nhất
    List<RevenueStats> getLast7Days();
    
    // Lấy 30 ngày gần nhất
    List<RevenueStats> getLast30Days();
    
    // Lấy dữ liệu DAILY của tháng hiện tại (để gom thành tuần)
    List<RevenueStats> getCurrentMonthDays();
    
    // Lấy dữ liệu MONTHLY của 3 tháng gần nhất
    List<RevenueStats> getLast3Months();
}