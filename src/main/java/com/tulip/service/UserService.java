package com.tulip.service;

import com.tulip.dto.UserProfileDTO;
import com.tulip.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User register(String email, String rawPassword, String fullName, String phone);

    // Lấy thông tin profile dưới dạng DTO
    UserProfileDTO getProfileByEmail(String email);

    // Cập nhật profile từ DTO
    void updateProfile(String email, UserProfileDTO dto, MultipartFile avatarFile);
    
    // Xác thực email bằng OTP
    boolean verifyEmail(String email, String otp);
    
    // Gửi lại OTP
    void resendOtp(String email);
}