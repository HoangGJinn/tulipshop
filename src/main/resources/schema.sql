-- Refresh Token Table for JWT Authentication
-- This file is automatically executed by Spring Boot on startup
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(2000) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stock History Table for Inventory Management Audit Trail
CREATE TABLE IF NOT EXISTS stock_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_id BIGINT NOT NULL,
    previous_quantity INT,
    new_quantity INT,
    change_amount INT,
    admin_username VARCHAR(255),
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (stock_id) REFERENCES product_stock(id) ON DELETE CASCADE,
    INDEX idx_stock_id (stock_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance Optimization Indexes for Inventory Management
-- Index on product_stock.sku for search queries
CREATE INDEX IF NOT EXISTS idx_product_stock_sku ON product_stock(sku);

-- Index on order_items.stock_id for reserved stock calculation
CREATE INDEX IF NOT EXISTS idx_order_items_stock_id ON order_items(stock_id);

-- Index on orders.status for filtering pending orders
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- Sample Vouchers for Testing
INSERT IGNORE INTO vouchers (code, name, description, type, discount_value, min_order_value, quantity, used_count, start_at, expire_at, status, is_public) VALUES
('WELCOME10', 'Voucher Chào Mừng 10%', 'Giảm 10% cho đơn hàng từ 100k', 'PERCENT', 10.00, 100000.00, 1000, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, 1),
('SAVE50K', 'Giảm 50K', 'Giảm 50.000đ cho đơn hàng từ 200k', 'AMOUNT', 50000.00, 200000.00, 500, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, 1),
('FREESHIP', 'Miễn Phí Vận Chuyển', 'Free ship cho đơn hàng từ 50k', 'AMOUNT', 20000.00, 50000.00, 0, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, 0),
('FREESHIP100', 'Miễn Phí Ship 100K', 'Free ship 30k cho đơn hàng từ 100k', 'AMOUNT', 30000.00, 100000.00, 1000, 0, '2024-01-01 00:00:00', '2026-12-31 23:59:59', 1, 1),
('SALE20', 'Giảm 20%', 'Giảm 20% cho đơn hàng từ 500k', 'PERCENT', 20.00, 500000.00, 100, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, 1);

-- Add admin reply fields to ratings table
ALTER TABLE ratings 
ADD COLUMN IF NOT EXISTS admin_reply TEXT,
ADD COLUMN IF NOT EXISTS reply_time TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS is_visible BOOLEAN DEFAULT TRUE;

-- Add index for admin queries
CREATE INDEX IF NOT EXISTS idx_ratings_created_at ON ratings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ratings_stars ON ratings(stars);
CREATE INDEX IF NOT EXISTS idx_ratings_is_visible ON ratings(is_visible);

-- Add shipping_fee column to revenue_stats table
ALTER TABLE revenue_stats 
ADD COLUMN IF NOT EXISTS shipping_fee DECIMAL(12,2) DEFAULT 0.00 COMMENT 'Tổng tiền ship (tách riêng khỏi doanh thu)';
