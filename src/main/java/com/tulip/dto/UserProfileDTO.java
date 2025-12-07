package com.tulip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;
    
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;
    
    @Pattern(regexp = "^$|^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;
    
    private String address; // Giữ lại để backward compatibility
    
    private String avatar; // Chỉ có khi response
    
    private Byte gender; // 0: Nữ, 1: Nam, 2: Khác
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    
    private List<UserAddressDTO> addresses; // Danh sách địa chỉ (chỉ có khi response)
}