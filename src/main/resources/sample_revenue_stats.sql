-- Insert dữ liệu giả lập cho revenue_stats
-- 50 bản ghi từ tháng 7/2025 đến 31/12/2025

INSERT INTO revenue_stats (stats_date, stats_type, revenue, order_count, customer_count, pending_orders, shipping_orders, delivered_orders, cancelled_orders, created_at, updated_at) VALUES
-- Tháng 7/2025
('2025-07-01', 'DAILY', 12500000, 45, 12, 5, 8, 30, 2, NOW(), NOW()),
('2025-07-05', 'DAILY', 18200000, 67, 18, 8, 12, 45, 2, NOW(), NOW()),
('2025-07-10', 'DAILY', 9800000, 32, 9, 3, 6, 22, 1, NOW(), NOW()),
('2025-07-15', 'DAILY', 22300000, 78, 21, 10, 15, 50, 3, NOW(), NOW()),
('2025-07-20', 'DAILY', 15600000, 54, 15, 6, 10, 36, 2, NOW(), NOW()),
('2025-07-25', 'DAILY', 28500000, 92, 25, 12, 18, 58, 4, NOW(), NOW()),
('2025-07-31', 'DAILY', 19800000, 61, 17, 7, 11, 41, 2, NOW(), NOW()),

-- Tháng 8/2025
('2025-08-01', 'DAILY', 14200000, 48, 13, 5, 9, 32, 2, NOW(), NOW()),
('2025-08-05', 'DAILY', 21500000, 72, 20, 9, 14, 47, 2, NOW(), NOW()),
('2025-08-10', 'DAILY', 11900000, 39, 11, 4, 7, 27, 1, NOW(), NOW()),
('2025-08-15', 'DAILY', 25800000, 85, 23, 11, 16, 55, 3, NOW(), NOW()),
('2025-08-20', 'DAILY', 17400000, 58, 16, 7, 11, 38, 2, NOW(), NOW()),
('2025-08-25', 'DAILY', 32100000, 98, 28, 13, 19, 62, 4, NOW(), NOW()),
('2025-08-31', 'DAILY', 23700000, 76, 21, 9, 14, 51, 2, NOW(), NOW()),

-- Tháng 9/2025
('2025-09-01', 'DAILY', 16800000, 55, 15, 6, 10, 37, 2, NOW(), NOW()),
('2025-09-05', 'DAILY', 24600000, 81, 22, 10, 15, 53, 3, NOW(), NOW()),
('2025-09-10', 'DAILY', 13500000, 44, 12, 5, 8, 30, 1, NOW(), NOW()),
('2025-09-15', 'DAILY', 29200000, 93, 26, 12, 18, 60, 3, NOW(), NOW()),
('2025-09-20', 'DAILY', 19700000, 65, 18, 8, 12, 43, 2, NOW(), NOW()),
('2025-09-25', 'DAILY', 35400000, 105, 30, 14, 20, 67, 4, NOW(), NOW()),
('2025-09-30', 'DAILY', 26300000, 84, 23, 10, 16, 56, 2, NOW(), NOW()),

-- Tháng 10/2025
('2025-10-01', 'DAILY', 18900000, 62, 17, 7, 12, 41, 2, NOW(), NOW()),
('2025-10-05', 'DAILY', 27500000, 88, 24, 11, 17, 58, 2, NOW(), NOW()),
('2025-10-10', 'DAILY', 15200000, 49, 14, 6, 9, 33, 1, NOW(), NOW()),
('2025-10-15', 'DAILY', 31800000, 97, 27, 13, 19, 62, 3, NOW(), NOW()),
('2025-10-20', 'DAILY', 22100000, 71, 20, 9, 13, 47, 2, NOW(), NOW()),
('2025-10-25', 'DAILY', 38700000, 112, 32, 15, 22, 71, 4, NOW(), NOW()),
('2025-10-31', 'DAILY', 28900000, 91, 25, 11, 17, 60, 3, NOW(), NOW()),

-- Tháng 11/2025 (Black Friday - doanh thu cao hơn)
('2025-11-01', 'DAILY', 21400000, 68, 19, 8, 13, 45, 2, NOW(), NOW()),
('2025-11-05', 'DAILY', 30200000, 95, 26, 12, 18, 62, 3, NOW(), NOW()),
('2025-11-10', 'DAILY', 17600000, 56, 16, 7, 10, 37, 2, NOW(), NOW()),
('2025-11-15', 'DAILY', 35500000, 108, 30, 14, 21, 69, 4, NOW(), NOW()),
('2025-11-20', 'DAILY', 24800000, 79, 22, 10, 15, 52, 2, NOW(), NOW()),
('2025-11-25', 'DAILY', 45200000, 135, 38, 18, 26, 87, 4, NOW(), NOW()),
('2025-11-28', 'DAILY', 52800000, 158, 45, 21, 30, 102, 5, NOW(), NOW()),
('2025-11-30', 'DAILY', 33600000, 102, 29, 13, 20, 66, 3, NOW(), NOW()),

-- Tháng 12/2025 (Giáng sinh & Tết Dương - cao điểm)
('2025-12-01', 'DAILY', 28700000, 89, 25, 11, 17, 58, 3, NOW(), NOW()),
('2025-12-05', 'DAILY', 36900000, 111, 31, 15, 21, 72, 3, NOW(), NOW()),
('2025-12-10', 'DAILY', 23400000, 74, 21, 9, 14, 49, 2, NOW(), NOW()),
('2025-12-15', 'DAILY', 41200000, 122, 34, 16, 23, 79, 4, NOW(), NOW()),
('2025-12-20', 'DAILY', 48500000, 142, 40, 19, 27, 92, 4, NOW(), NOW()),
('2025-12-24', 'DAILY', 55300000, 165, 47, 22, 31, 107, 5, NOW(), NOW()),
('2025-12-25', 'DAILY', 58900000, 175, 50, 24, 33, 113, 5, NOW(), NOW()),
('2025-12-26', 'DAILY', 46800000, 138, 39, 18, 26, 90, 4, NOW(), NOW()),
('2025-12-30', 'DAILY', 39200000, 116, 33, 15, 22, 76, 3, NOW(), NOW()),
('2025-12-31', 'DAILY', 42600000, 125, 35, 17, 24, 81, 3, NOW(), NOW());

-- Insert stats theo tuần (một số tuần)
INSERT INTO revenue_stats (stats_date, stats_type, revenue, order_count, customer_count, pending_orders, shipping_orders, delivered_orders, cancelled_orders, created_at, updated_at) VALUES
('2025-08-31', 'WEEKLY', 145800000, 489, 135, 56, 89, 328, 16, NOW(), NOW()),
('2025-09-30', 'WEEKLY', 158200000, 512, 142, 61, 95, 342, 14, NOW(), NOW()),
('2025-10-31', 'WEEKLY', 182500000, 568, 158, 68, 105, 378, 17, NOW(), NOW());

-- Cập nhật ngày hôm nay (2026-01-01) với dữ liệu ban đầu
INSERT INTO revenue_stats (stats_date, stats_type, revenue, order_count, customer_count, pending_orders, shipping_orders, delivered_orders, cancelled_orders, created_at, updated_at) 
VALUES ('2026-01-01', 'DAILY', 0, 0, 0, 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
