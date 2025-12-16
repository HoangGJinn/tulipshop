package com.tulip.service.impl;

import lombok.Setter;
import org.springframework.security.core.userdetails.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Getter
@Setter
public class CustomUserDetails extends User {

    private final Long userId;
    private String fullName;
    private String email;
    private String avatar;
    private LocalDate birthday;
    private LocalDateTime emailVerifiedAt; // Thêm field để kiểm tra email đã verify chưa

    // 2. Chỗ tham số truyền vào: Phải ghi rõ đường dẫn "com.tulip.entity.User"
    // Để Java phân biệt được với class "User" (của Spring) đang được kế thừa
    public CustomUserDetails(com.tulip.entity.User user) {
        super(
                user.getEmail(),
                // noop là để Spring Security biết là password này không được mã hóa
                user.getPasswordHash() != null ? user.getPasswordHash() : "{noop}",
                user.getStatus(),
                true,
                true,
                true,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().getValue()))
        );

        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getProfile() != null ? user.getProfile().getFullName() : null;
        this.avatar = user.getProfile() != null ? user.getProfile().getAvatar() : null;
        this.birthday = user.getProfile() != null ? user.getProfile().getBirthday() : null;
        this.emailVerifiedAt = user.getEmailVerifiedAt(); // Lưu emailVerifiedAt
    }
}