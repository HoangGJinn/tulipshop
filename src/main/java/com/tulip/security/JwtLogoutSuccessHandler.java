package com.tulip.security;

import com.tulip.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutSuccessHandler implements LogoutSuccessHandler {
    
    private final RefreshTokenService refreshTokenService;
    
    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                               HttpServletResponse response,
                               Authentication authentication) throws IOException {
        
        // Revoke refresh token nếu có
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshTokenService.revokeToken(cookie.getValue());
                    break;
                }
            }
        }
        
        // Xóa cookies
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");
        
        response.sendRedirect("/login?logout");
    }
    
    /**
     * Xóa cookie bằng cách set maxAge=0 và SameSite để đảm bảo browser xóa cookie
     */
    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();
        
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Cookie '{}' deleted", name);
    }
}


