package com.tulip.config;

import com.tulip.service.impl.CustomOAuth2UserService;
import com.tulip.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableWebSecurity // Khong bắt buộc nhưng nên thêm để đánh dấu đây là cấu hình bảo mật
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    // DataSource: Là một cái Interface (Bản vẽ thiết kế). 
    // Nó định nghĩa các hành động: Kết nối đến database, Lấy connection, Close connection, ...
    // DataSource được sử dụng để kết nối đến database và lưu trữ các token remember-me.
    private final DataSource dataSource;
    private final CustomOAuth2UserService oauth2UserService;
    
    @Value("${security.remember-me.key}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // UsernamePasswordAuthenticationFilter đã lấy password từ form login
    // và truyền nó cho DaoAuthenticationProvider để kiểm tra
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Persistent Token Repository for Remember-Me functionality
     * Uses database to store remember-me tokens (more secure than cookie-only approach)
     */
    @Bean
    // PersistentTokenRepository: Là một cái Interface (Bản vẽ thiết kế). 
    // Nó định nghĩa các hành động: Tạo token mới, Cập nhật token, Lấy token ra xem, Xóa token.
    public PersistentTokenRepository persistentTokenRepository() {
        // jdbcTokenRepositoryImpl Là class thực thi (Người thợ). 
        // Class này đã được Spring viết sẵn code SQL bên trong (INSERT, UPDATE, DELETE bảng persistent_logins).
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        // Table will be created by schema.sql on startup
        return tokenRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/static/**", "/assets/**", "/favicon.ico").permitAll()
                .requestMatchers("/register", "/login", "/logout", "/api/**" ,"/h2-console/**").permitAll()
                .requestMatchers("/products/**", "/product/**", "/trending", "/sale", "/about", "/contact").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
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
                    .defaultSuccessUrl("/", true)
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
            .rememberMe(remember -> remember
                .tokenRepository(persistentTokenRepository())
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 604800 seconds = 7 days
                .key(rememberMeKey) // Secure key for token signing (from application.properties)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", "remember-me") // Delete both session and remember-me cookies
                .permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }
}
