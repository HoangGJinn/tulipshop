package com.tulip.service.impl;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Custom OAuth2User implementation với các field thống nhất với CustomUserDetails
 * Các field: userId, fullName, email, avatar, birthday, enabled
 */
@Getter
public class CustomOAuth2User implements OAuth2User {
    
    private final Long userId;
    private final String fullName;
    private final String email;
    private final String avatar;
    private final LocalDate birthday;
    private final Boolean enabled; // Trạng thái kích hoạt (dùng status từ User entity)
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String name; // OAuth2User requires this, it is usually the username or email

    public CustomOAuth2User(com.tulip.entity.User user, Map<String, Object> attributes) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getProfile() != null ? user.getProfile().getFullName() : null;
        this.avatar = user.getProfile() != null ? user.getProfile().getAvatar() : null;
        this.birthday = user.getProfile() != null ? user.getProfile().getBirthday() : null;
        this.enabled = user.getStatus(); // Dùng status làm enabled
        this.attributes = attributes;
        this.name = user.getEmail(); // OAuth2User requires name attribute
        
        // Tạo authorities từ role
        this.authorities = Collections.singleton(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().getValue())
        );
    }
    
    /**
     * Kiểm tra tài khoản có được phép đăng nhập không
     * Spring Security sẽ gọi method này để kiểm tra
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return name;
    }
}

