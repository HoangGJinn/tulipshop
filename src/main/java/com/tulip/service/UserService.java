package com.tulip.service;

import com.tulip.dto.UserProfileDTO;
import com.tulip.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {
    User register(String email, String rawPassword, String fullName, String phone);
    
    // Kiểm tra email đã tồn tại
    boolean existsByEmail(String email);

    // Lấy thông tin profile dưới dạng DTO
    UserProfileDTO getProfileByEmail(String email);

    // Cập nhật profile từ DTO
    void updateProfile(String email, UserProfileDTO dto, MultipartFile avatarFile);
    
    // Xác thực email bằng OTP
    boolean verifyEmail(String email, String otp);
    
    // Gửi lại OTP
    void resendOtp(String email, String type);

    // Gửi OTP để reset password
    void sendPasswordResetOtp(String email, String type);

    // Đặt lại mật khẩu với OTP
    void resetPassword(String email, String otp, String newPassword);
    
    // Thay đổi mật khẩu (cần mật khẩu cũ)
    void changePassword(String email, String oldPassword, String newPassword);
    
    // Tạo mật khẩu cho user Google (không cần mật khẩu cũ)
    void setPassword(String email, String newPassword);
    
    // Lấy thông tin authProvider và hasPassword
    Map<String, Object> getPasswordInfo(String email);
}