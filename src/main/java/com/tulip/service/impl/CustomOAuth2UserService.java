package com.tulip.service.impl;

import com.tulip.entity.Role;
import com.tulip.entity.User;
import com.tulip.entity.UserProfile;
import com.tulip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            String email = oauth2User.getAttribute("email");
            String fullName = oauth2User.getAttribute("name"); // Google trả về "name" nhưng ta đặt tên biến là fullName cho thống nhất
            String picture = oauth2User.getAttribute("picture"); // Avatar URL từ Google
            String birthdayStr = oauth2User.getAttribute("birthday"); // Birthday từ Google (format: "YYYY-MM-DD" hoặc null)
            String provider = userRequest.getClientRegistration().getRegistrationId();

            // Kiểm tra email có null không
            if (email == null || email.isEmpty()) {
                log.error("Email is null or empty from OAuth2 provider: {}", provider);
                throw new OAuth2AuthenticationException("Email không thể lấy được từ Google");
            }

            // Parse birthday nếu có (Google trả về format "YYYY-MM-DD" hoặc null)
            java.time.LocalDate birthday = null;
            if (birthdayStr != null && !birthdayStr.isEmpty()) {
                try {
                    birthday = java.time.LocalDate.parse(birthdayStr);
                } catch (Exception e) {
                    log.warn("Cannot parse birthday from Google: {}", birthdayStr);
                }
            }

            log.info("OAuth2 Login - Email: {}, FullName: {}, Avatar: {}, Birthday: {}, Provider: {}", 
                    email, fullName, picture, birthday, provider);

            User existingUser = userRepository.findByEmail(email).orElse(null);

            if (existingUser == null) {
                // Tạo user mới
                User newUser = User.builder()
                        .email(email)
                        .passwordHash(null)
                        .authProvider("GOOGLE")
                        .role(Role.CUSTOMER)
                        .status(true)
                        .emailVerifiedAt(LocalDateTime.now())
                        .build();

                UserProfile profile = UserProfile.builder()
                        .fullName(fullName != null ? fullName : email) // Fallback nếu fullName null
                        .avatar(picture) // Lưu avatar từ Google
                        .birthday(birthday) // Lưu birthday từ Google (nếu có)
                        .build();
                newUser.setProfile(profile);

                existingUser = userRepository.save(newUser);
                log.info("Created new OAuth2 user with verified email: {}", email);
            } else {
                // User đã tồn tại - cập nhật nếu cần
                if (existingUser.getAuthProvider() == null || existingUser.getAuthProvider().equals("LOCAL")) {
                    existingUser.setAuthProvider("GOOGLE");
                }

                // Nếu chưa có emailVerifiedAt (User đăng ký LOCAL trước đó rồi đăng nhập Google, fb..)
                if (existingUser.getEmailVerifiedAt() == null) {
                    existingUser.setEmailVerifiedAt(LocalDateTime.now());
                    log.info("Email verified automatically for existing OAuth2 user: {}", email);
                }

                // Cập nhật profile nếu cần
                UserProfile profile = existingUser.getProfile();
                if (profile == null) {
                    profile = UserProfile.builder()
                            .fullName(fullName != null ? fullName : email)
                            .avatar(picture)
                            .birthday(birthday)
                            .build();
                    existingUser.setProfile(profile);
                } else {
                    if ((profile.getFullName() == null || profile.getFullName().isEmpty()) && fullName != null) {
                        profile.setFullName(fullName);
                    }
                    if ((profile.getAvatar() == null || profile.getAvatar().isEmpty()) && picture != null) {
                        profile.setAvatar(picture);
                    }
                    if (profile.getBirthday() == null && birthday != null) {
                        profile.setBirthday(birthday);
                    }
                }

                existingUser = userRepository.save(existingUser);
                log.info("OAuth2 user already exists: {}", email);
            }

            // Kiểm tra status (enabled) - nếu false thì không cho đăng nhập
            if (existingUser.getStatus() == null || !existingUser.getStatus()) {
                log.warn("OAuth2 login blocked - User account is disabled: {}", email);
                throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("account_disabled",
                            "Tài khoản này đã bị khóa hoặc chưa được kích hoạt", null)
                );
            }

            // Return CustomOAuth2User với các field thống nhất: userId, fullName, email, avatar, birthday, enabled
            return new CustomOAuth2User(existingUser, oauth2User.getAttributes());

        } catch (OAuth2AuthenticationException e) {
            // Nếu đã là OAuth2AuthenticationException thì throw lại luôn, không wrap
            throw e;
} catch (Exception e) {
    log.error("Error processing OAuth2 user", e);
    throw new OAuth2AuthenticationException(
            new org.springframework.security.oauth2.core.OAuth2Error("oauth2_error",
                    "Lỗi xử lý đăng nhập Google: " + e.getMessage(), null)
    );
}
    }
}