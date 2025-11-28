package com.tulip.service.impl;

import com.tulip.entity.User;
import com.tulip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    // Đây là interface method từ UserDetailsService, nên không được đổi tên
    // vì func này ngầm định param đc truyền vào là username (ở đây ta dùng email làm username)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        String role = user.getRole();
        String roleWithPrefix = (role != null && role.startsWith("ROLE_") ? role : "ROLE_" + role);
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(roleWithPrefix));

        // Đây là class User có sẵn của Spring, không phải Entity com.tulip.entity.User của bạn.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(!user.getStatus()) // Nếu status = false thì tài khoản bị khóa
                .build();
    }
}
