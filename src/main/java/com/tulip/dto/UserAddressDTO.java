package com.tulip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDTO {
    private Long id; // null khi create, có giá trị khi update
    
    @NotBlank(message = "Họ và tên người nhận không được để trống")
    private String recipientName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String recipientPhone;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    private String addressLine;
    
    private String village;
    private String district;
    private String province;
    
    @Builder.Default
    private Boolean isDefault = false;
    
    private String fullAddress; // Địa chỉ đầy đủ (computed, chỉ có khi response)
}

