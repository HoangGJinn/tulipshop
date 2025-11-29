package com.tulip.service.impl;

import lombok.Setter;
import org.springframework.security.core.userdetails.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Getter
@Setter
public class CustomUserDetails extends User {

    private final Long userId;
    private String fullName;
    private String email;

    // 2. Chỗ tham số truyền vào: Phải ghi rõ đường dẫn "com.tulip.entity.User"
    // Để Java phân biệt được với class "User" (của Spring) đang được kế thừa
    public CustomUserDetails(com.tulip.entity.User user) {
        super(
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus(), // Lưu ý: status trong DB không được null
                true,
                true,
                true,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().getValue()))
        );

        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getProfile() != null ? user.getProfile().getFullName() : null;
    }
}