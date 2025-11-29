package com.tulip.service.impl;

import com.tulip.entity.Role;
import com.tulip.entity.User;
import com.tulip.entity.UserProfile;
import com.tulip.repository.UserRepository;
import com.tulip.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(String email, String rawPassword, String fullName, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email đã được sử dụng");
        }
        User user = User.builder()          // 1. Khởi động nhà máy (Static method)
                .email(email)               // 2. Set giá trị email, trả về Builder
                .passwordHash(passwordEncoder.encode(rawPassword))  // 3. Set pass, trả về Builder
                .role(Role.CUSTOMER)        // 4. Set role, trả về Builder
                .status(true)               // 5. Set status, trả về Builder
                .build();                   // 6. Trả về đối tượng User hoàn chỉnh ~ new User()

        UserProfile profile = UserProfile.builder()
                .fullName(fullName)
                .phone(phone != null && !phone.trim().isEmpty() ? phone : null)
                .build();
        user.setProfile(profile);

        return userRepository.save(user);
    }
}
