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
INSERT IGNORE INTO vouchers (code, type, discount_value, min_order_value, quantity, used_count, start_at, expire_at, status, created_at) VALUES
('WELCOME10', 'PERCENT', 10.00, 100000.00, 1000, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, NOW()),
('SAVE50K', 'AMOUNT', 50000.00, 200000.00, 500, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, NOW()),
('FREESHIP', 'AMOUNT', 30000.00, 50000.00, 2000, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, NOW()),
('SALE20', 'PERCENT', 20.00, 500000.00, 100, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 1, NOW());

