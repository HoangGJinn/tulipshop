package com.tulip.config;

import com.tulip.service.impl.CustomOAuth2User;
import com.tulip.service.impl.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${api.base-url}")
    private String apiBaseUrl;

    @ModelAttribute("apiBaseUrl")
    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    @ModelAttribute("userName")
    public String userName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = null;

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();

            // Đăng nhập normal (username/password) – dùng CustomUserDetails
            if (principal instanceof CustomUserDetails details) {
                userName = details.getFullName() != null && !details.getFullName().isEmpty()
                    ? details.getFullName()
                    : details.getEmail();
            }
            // Đăng nhập OAuth2 – dùng CustomOAuth2User
            else if (principal instanceof CustomOAuth2User oauth2User) {
                userName = oauth2User.getFullName() != null && !oauth2User.getFullName().isEmpty()
                    ? oauth2User.getFullName()
                    : oauth2User.getEmail();
            }
            // Fallback: principal là String (username)
            else if (principal instanceof String s) {
                userName = s;
            }
        }

        // Giá trị mặc định nếu không xác định được
        if (userName == null || userName.isEmpty()) {
            userName = "Admin";
        }

        return userName;
    }
}

