package com.tulip.entity.enums;

public enum PaymentStatus {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    EXPIRED("EXPIRED"),
    CANCELLED("CANCELLED");
    
    private final String value;
    
    PaymentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
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


