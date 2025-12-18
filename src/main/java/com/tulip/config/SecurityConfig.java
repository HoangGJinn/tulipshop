package com.tulip.config;

import com.tulip.security.JwtAuthenticationFilter;
import com.tulip.security.JwtAuthenticationSuccessHandler;
import com.tulip.security.JwtLogoutSuccessHandler;
import com.tulip.security.JwtOAuth2SuccessHandler;
import com.tulip.service.impl.CustomOAuth2UserService;
import com.tulip.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Configuration
@EnableWebSecurity // Khong bắt buộc nhưng nên thêm để đánh dấu đây là cấu hình bảo mật
@RequiredArgsConstructor
public class SecurityConfig {

    // Spring Security sẽ tự động sử dụng UserDetailsService bean để cấu hình authentication
    // Field này được giữ lại để đảm bảo bean được tạo trước khi SecurityConfig được khởi tạo
    @SuppressWarnings("unused")
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oauth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final JwtOAuth2SuccessHandler jwtOAuth2SuccessHandler;
    private final JwtLogoutSuccessHandler jwtLogoutSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Spring Security sẽ tự động cấu hình DaoAuthenticationProvider từ UserDetailsService và PasswordEncoder
        return config.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/static/**", "/assets/**", "/favicon.ico").permitAll()
                .requestMatchers("/register", "/login", "/logout","/api/**" , "/h2-console/**").permitAll()
                .requestMatchers("/verify-email", "/resend-otp").permitAll()
                .requestMatchers("/forgot-password", "/reset-password").permitAll()
                .requestMatchers("/products/**", "/product/**", "/trending", "/sale", "/about", "/contact").permitAll()
                .requestMatchers("/v1/api/store/**").permitAll()
                .requestMatchers("/v1/api/auth/login", "/v1/api/auth/register", "/v1/api/auth/forgot-password", 
                                 "/v1/api/auth/reset-password", "/v1/api/auth/resend-otp", "/v1/api/auth/verify-email").permitAll()
                .requestMatchers("/v1/api/auth/**").authenticated()
                .requestMatchers("/error/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                    // VNPAY callback URL cho phép truy cập công khai vì không có JWT:
                    .requestMatchers("/v1/api/vnpay/payment-callback").permitAll()
                    // CHỈ người dùng đã đăng nhập mới được mua hàng:
                    .requestMatchers("/v1/api/vnpay/create-payment").authenticated()
                .anyRequest().authenticated()
            )
            // Xử lý exception: Nếu đã authenticated nhưng không có quyền hoặc URL không tồn tại
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    // Kiểm tra xem có JWT token trong request không
                    String authHeader = request.getHeader("Authorization");
                    Cookie[] cookies = request.getCookies();
                    boolean hasToken = (authHeader != null && authHeader.startsWith("Bearer ")) ||
                                     (cookies != null && java.util.Arrays.stream(cookies)
                                         .anyMatch(c -> "accessToken".equals(c.getName()) || "refreshToken".equals(c.getName())));
                    
                    Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();
                    
                    // Nếu đã có authentication
                    if (auth != null && auth.isAuthenticated()) {
                        // Đã authenticated, để Spring MVC xử lý 404
                        // Không redirect, để request tiếp tục và Spring MVC sẽ xử lý
                        // Nhưng vì authenticationEntryPoint được gọi, có nghĩa là có vấn đề với authorization
                        // Nên để request tiếp tục và Spring MVC sẽ xử lý 404 hoặc 403
                        return;
                    }
                    
                    // Nếu có token trong request nhưng không hợp lệ, vẫn để Spring MVC xử lý
                    // (có thể là 404 nếu URL không tồn tại, hoặc có thể là token hết hạn)
                    if (hasToken) {
                        // Có token, để Spring MVC xử lý (không redirect về login)
                        return;
                    }
                    
                    // Chưa authenticated và không có token, redirect về login
                    response.sendRedirect("/login");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Nếu đã authenticated nhưng không có quyền, hiển thị trang 403
                    Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();
                    if (auth != null && auth.isAuthenticated()) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            )
            // Thêm JWT filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(jwtAuthenticationSuccessHandler) // Tạo JWT tokens
                .failureHandler((request, response, exception) -> {
                    if (exception instanceof DisabledException) {
                        response.sendRedirect("/login?disabled");
                    } else {
                        response.sendRedirect("/login?error");
                    }
                })
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService)
                )
                .successHandler(jwtOAuth2SuccessHandler) // Tạo JWT cho OAuth2
                .failureHandler((request, response, exception) -> {
                    String errorMessage = null;
                    
                    if (exception instanceof OAuth2AuthenticationException) {
                        OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
                        if (oauth2Exception.getError() != null) {
                            errorMessage = oauth2Exception.getError().getDescription();
                        }
                        if (errorMessage == null) {
                            errorMessage = oauth2Exception.getMessage();
                        }
                    } else {
                        errorMessage = exception.getMessage();
                    }
                    
                    log.error("OAuth2 login failed: {}", errorMessage, exception);
                    
                    if (errorMessage != null && 
                        (errorMessage.contains("bị khóa") || 
                         errorMessage.contains("chưa được kích hoạt"))) {
                        response.sendRedirect("/login?disabled");
                    } else {
                        response.sendRedirect("/login?oauth2Error=true");
                    }
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(jwtLogoutSuccessHandler) // Xóa JWT cookies
                .permitAll()
            )
            // Spring Security sẽ tự động sử dụng UserDetailsService và PasswordEncoder beans để cấu hình authentication
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }
}
