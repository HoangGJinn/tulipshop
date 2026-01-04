-- Migration script to add quantity and used_count columns to vouchers table
-- Run this if you already have a vouchers table without these columns

ALTER TABLE vouchers 
ADD COLUMN IF NOT EXISTS quantity INT DEFAULT 100,
ADD COLUMN IF NOT EXISTS used_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing vouchers to have default values
UPDATE vouchers SET quantity = 100 WHERE quantity IS NULL;
UPDATE vouchers SET used_count = 0 WHERE used_count IS NULL;
