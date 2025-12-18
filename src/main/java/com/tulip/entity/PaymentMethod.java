package com.tulip.entity;

public enum PaymentMethod {
    COD("COD"),
    VNPAY("VNPAY"),
    MOMO("MOMO");
    
    private final String value;
    
    PaymentMethod(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Chuyển đổi từ String sang PaymentMethod enum
     * @param value giá trị String
     * @return PaymentMethod enum, mặc định là COD nếu không tìm thấy
     */
    public static PaymentMethod fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return COD;
        }
        
        String normalizedValue = value.toUpperCase().trim();
        
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.value.equalsIgnoreCase(normalizedValue)) {
                return method;
            }
        }
        
        return COD; // Default
    }
}

