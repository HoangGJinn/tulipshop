package com.tulip.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tulip.dto.UserProfileDTO;
import com.tulip.entity.Role;
import com.tulip.entity.User;
import com.tulip.entity.UserProfile;
import com.tulip.mapper.UserProfileMapper;
import com.tulip.repository.UserRepository;
import com.tulip.repository.UserProfileRepository;
import com.tulip.service.EmailService;
import com.tulip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;
    private final UserProfileMapper userProfileMapper; // Inject mapper
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional
    public User register(String email, String rawPassword, String fullName, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email đã được sử dụng");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .authProvider("LOCAL")
                .role(Role.CUSTOMER)
                .status(true)
                .emailVerifiedAt(null) // Chưa xác thực email
                .build();

        UserProfile profile = UserProfile.builder()
                .fullName(fullName)
                .phone(phone != null && !phone.trim().isEmpty() ? phone : null)
                .build();
        user.setProfile(profile);

        user = userRepository.save(user);
        
        // Gửi OTP để xác thực email
        String otp = otpService.generateOtp(email);
        emailService.sendOTPToEmail(email, otp, "verify");
        log.info("OTP sent to email: {}", email);
        
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Dùng mapper để convert
        return userProfileMapper.toDTO(user);
    }

    @Override
    @Transactional
    public void updateProfile(String email, UserProfileDTO dto, MultipartFile avatarFile) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder()
                    .user(user)
                    .build();
        }

        // Dùng mapper để update entity từ DTO
        userProfileMapper.updateEntityFromDTO(profile, dto);

        // Upload avatar nếu có
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                        avatarFile.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "tulipshop/avatars",
                                "public_id", "user_" + user.getId(),
                                "overwrite", true,
                                "resource_type", "image"
                        )
                );
                String avatarUrl = (String) uploadResult.get("secure_url");
                profile.setAvatar(avatarUrl);
            } catch (IOException e) {
                log.error("Error uploading avatar", e);
                throw new RuntimeException("Không thể upload ảnh đại diện", e);
            }
        }

        userProfileRepository.save(profile);
    }
    
    @Override
    @Transactional
    public boolean verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Email không tồn tại"));
        
        // Kiểm tra OTP
        if (!otpService.validateOtp(email, otp)) {
            return false;
        }
        
        // Nếu OTP đúng, cập nhật emailVerifiedAt
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Email verified successfully for: {}", email);
        }
        
        return true;
    }
    
    @Override
    public void resendOtp(String email, String type) {
        if (!userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email không tồn tại");
        }
        
        // Xác định key cho OTP dựa trên type
        String otpKey = "reset".equals(type) ? "reset_" + email : email;
        
        // Xóa OTP cũ nếu có
        otpService.clearOtp(otpKey);
        
        // Tạo và gửi OTP mới
        String otp = otpService.generateOtp(otpKey);
        emailService.sendOTPToEmail(email, otp, type);
        log.info("{} OTP resent to email: {}", type, email);
    }

    @Override
    @Transactional
    public void sendPasswordResetOtp(String email, String type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Email không tồn tại trong hệ thống"));
        
        // Kiểm tra user có phải đăng ký bằng LOCAL không
        if ("reset".equals(type) && !"LOCAL".equals(user.getAuthProvider())) {
            String provider = "GOOGLE".equals(user.getAuthProvider()) ? "Google" : "OAuth2";
            throw new IllegalStateException("Tài khoản này được đăng ký bằng " + provider + ". Vui lòng đăng nhập bằng " + provider + ".");
        }
        
        // Xác định key cho OTP dựa trên type
        String otpKey = "reset".equals(type) ? "reset_" + email : email;
        
        // Xóa OTP cũ nếu có
        otpService.clearOtp(otpKey);
        
        // Tạo và gửi OTP mới
        String otp = otpService.generateOtp(otpKey);
        emailService.sendOTPToEmail(email, otp, type);
        log.info("{} OTP sent to email: {}", type, email);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        // Kiểm tra email có tồn tại không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Email không tồn tại trong hệ thống"));
        
        // Kiểm tra user có phải đăng ký bằng LOCAL không
        if (!"LOCAL".equals(user.getAuthProvider())) {
            String provider = "GOOGLE".equals(user.getAuthProvider()) ? "Google" : "OAuth2";
            throw new IllegalStateException("Tài khoản này được đăng ký bằng " + provider + ". Vui lòng đăng nhập bằng " + provider + ".");
        }
        
        // Validate OTP
        if (!otpService.validateOtp("reset_" + email, otp)) {
            throw new IllegalStateException("Mã OTP không đúng hoặc đã hết hạn");
        }
        
        // Validate password mới
        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            throw new IllegalStateException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        
        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully for email: {}", email);
    }

    @Override
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        // Kiểm tra email có tồn tại không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Email không tồn tại trong hệ thống"));
        
        // Kiểm tra user có phải đăng ký bằng LOCAL không
        if (!"LOCAL".equals(user.getAuthProvider())) {
            String provider = "GOOGLE".equals(user.getAuthProvider()) ? "Google" : "OAuth2";
            throw new IllegalStateException("Tài khoản này được đăng ký bằng " + provider + ". Không thể thay đổi mật khẩu.");
        }
        
        // Kiểm tra mật khẩu cũ
        if (user.getPasswordHash() == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalStateException("Mật khẩu cũ không đúng");
        }
        
        // Validate mật khẩu mới
        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            throw new IllegalStateException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        
        // Kiểm tra mật khẩu mới không được trùng với mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalStateException("Mật khẩu mới phải khác mật khẩu cũ");
        }
        
        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for email: {}", email);
    }

    @Override
    @Transactional
    public void setPassword(String email, String newPassword) {
        // Kiểm tra email có tồn tại không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Email không tồn tại trong hệ thống"));
        
        // Validate mật khẩu mới
        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            throw new IllegalStateException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        
        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password set successfully for email: {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPasswordInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> info = new HashMap<>();
        // Đảm bảo authProvider không null (mặc định là LOCAL)
        String authProvider = user.getAuthProvider();
        if (authProvider == null || authProvider.trim().isEmpty()) {
            authProvider = "LOCAL";
        }
        info.put("authProvider", authProvider);
        info.put("hasPassword", user.getPasswordHash() != null && !user.getPasswordHash().isEmpty());
        
        return info;
    }
}