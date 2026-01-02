package com.tulip.controller;

import com.tulip.security.JwtUtil;
import com.tulip.service.impl.CustomUserDetailsService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller xử lý các trang lỗi (404, 403, 500, etc.)
 * Đảm bảo authentication context được restore từ JWT cookie nếu cần
 */
@Controller
@RequiredArgsConstructor
public class CustomErrorController implements ErrorController {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Restore authentication từ JWT cookie nếu SecurityContext không có
        // Header fragment sử dụng #authentication trực tiếp từ Spring Security,
        restoreAuthenticationFromCookie(request);
        
        // Xác định error view dựa trên status code
        return getErrorView(request, model);
    }

    /**
     * Restore authentication từ JWT cookie nếu SecurityContext không có
     * Logic tương tự JwtAuthenticationFilter nhưng cần thiết cho error pages
     * vì filter có thể không chạy khi Spring Boot forward đến /error
     */
    private void restoreAuthenticationFromCookie(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu đã có authentication hợp lệ, không cần restore
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null 
            && !"anonymousUser".equals(auth.getPrincipal())) {
            return;
        }
        
        String jwt = jwtUtil.extractTokenFromRequest(request);
        
        // Validate và set authentication
        if (jwt != null) {
            try {
                String username = jwtUtil.extractUsername(jwt);
                String tokenType = jwtUtil.extractTokenType(jwt);
                
                // Chỉ xử lý ACCESS token
                if ("ACCESS".equals(tokenType) && username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // JWT không hợp lệ, bỏ qua
            }
        }
    }

    /**
     * Xác định error view dựa trên HTTP status code
     */
    private String getErrorView(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            switch (statusCode) {
                case 404:
                    model.addAttribute("title", "404 - Trang không tìm thấy");
                    return "error/404";
                case 403:
                    model.addAttribute("title", "403 - Không có quyền truy cập");
                    return "error/403";
                case 500:
                    model.addAttribute("title", "500 - Lỗi máy chủ");
                    return "error/500";
            }
        }
        
        model.addAttribute("title", "Lỗi");
        return "error/error";
    }
}

