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
import com.tulip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
                .build();

        UserProfile profile = UserProfile.builder()
                .fullName(fullName)
                .phone(phone != null && !phone.trim().isEmpty() ? phone : null)
                .build();
        user.setProfile(profile);

        return userRepository.save(user);
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
                Map<String, Object> uploadResult = cloudinary.uploader().upload(
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
}