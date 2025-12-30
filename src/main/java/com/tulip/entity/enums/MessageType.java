package com.tulip.entity.enums;

public enum MessageType {
    TEXT,       // Tin nhắn văn bản
    JOIN,       // Người dùng tham gia phòng
    LEAVE,      // Người dùng rời phòng
    TYPING,     // Đang gõ (không lưu DB)
    SEEN,       // Đã xem (chỉ update flag)
    SYSTEM      // Tin nhắn hệ thống
}

