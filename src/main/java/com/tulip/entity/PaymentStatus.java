package com.tulip.entity;

public enum PaymentStatus {
    PENDING("PENDING"),   // Khách đã tạo link nhưng chưa trả tiền
    SUCCESS("SUCCESS"),   // Nhà cung cấp thanh toán báo đã nhận tiền thành công
    FAILED("FAILED");     // Giao dịch lỗi hoặc khách hủy thanh toán
    
    private final String value;
    
    PaymentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Chuyển đổi từ String sang PaymentStatus enum
     * @param value giá trị String
     * @return PaymentStatus enum, mặc định là PENDING nếu không tìm thấy
     */
    public static PaymentStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        
        String normalizedValue = value.toUpperCase().trim();
        
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.value.equalsIgnoreCase(normalizedValue)) {
                return status;
            }
        }
        
        return PENDING; // Default
    }
}

