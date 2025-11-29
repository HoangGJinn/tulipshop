-- Spring Security Persistent Token Repository Table
-- This table stores remember-me tokens for users
-- This file is automatically executed by Spring Boot on startup

CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) NOT NULL,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL,
    PRIMARY KEY (series)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for faster lookups
CREATE INDEX idx_persistent_logins_username ON persistent_logins(username);
CREATE INDEX idx_persistent_logins_last_used ON persistent_logins(last_used);

