package com.tulip.entity.enums;

public enum OrderStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    SHIPPING("SHIPPING"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    RETURNED("RETURNED");
    
    private final String value;
    
    OrderStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static OrderStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        
        String normalizedValue = value.toUpperCase().trim();
        
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equalsIgnoreCase(normalizedValue)) {
                return status;
            }
        }
        
        return PENDING; // Default
    }
}

