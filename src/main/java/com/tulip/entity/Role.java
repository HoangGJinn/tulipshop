package com.tulip.entity;

public enum Role {
    CUSTOMER("CUSTOMER"),
    ADMIN("ADMIN");
    
    private final String value;
    
    Role(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Chuyển đổi từ String sang Role enum
     * @param value giá trị String (có thể có hoặc không có prefix "ROLE_")
     * @return Role enum, mặc định là CUSTOMER nếu không tìm thấy
     */
    public static Role fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CUSTOMER;
        }
        
        // Loại bỏ prefix "ROLE_" nếu có
        String normalizedValue = value.toUpperCase();
        if (normalizedValue.startsWith("ROLE_")) {
            normalizedValue = normalizedValue.substring(5);
        }
        
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(normalizedValue)) {
                return role;
            }
        }
        
        return CUSTOMER; // Default
    }
}

