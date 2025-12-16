package com.tulip.service.impl;

import com.tulip.entity.User;
import com.tulip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

        // Sử dụng CustomUserDetails để lưu userId và fullName
        return new CustomUserDetails(user);
    }
}
