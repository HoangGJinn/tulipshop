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
        emailService.sendOTPToEmail(email, otp);
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
    public void resendOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email không tồn tại");
        }
        
        // Xóa OTP cũ nếu có
        otpService.clearOtp(email);
        
        // Tạo và gửi OTP mới
        String otp = otpService.generateOtp(email);
        emailService.sendOTPToEmail(email, otp);
        log.info("OTP resent to email: {}", email);
    }
}