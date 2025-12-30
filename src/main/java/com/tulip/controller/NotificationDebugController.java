package com.tulip.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class NotificationDebugController {
    
    /**
     * Trang test notification bell
     * Truy cập: http://localhost:8080/test-notification
     */
    @GetMapping("/test-notification")
    public String testNotification(Model model, Authentication auth) {
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());
        if (auth != null) {
            model.addAttribute("username", auth.getName());
        }
        return "test-notification";
    }
    
    /**
     * Trang test toast notification
     * Truy cập: http://localhost:8080/test-toast
     */
    @GetMapping("/test-toast")
    public String testToast() {
        return "test-toast";
    }
}
