package com.tulip.controller.admin;

import com.tulip.dto.response.DashboardStatsDTO;
import com.tulip.entity.enums.OrderStatus;
import com.tulip.service.DashboardService;
import com.tulip.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminViewController {
    
    private final OrderService orderService;
    private final DashboardService dashboardService;

    // Xử lý route /admin - redirect đến dashboard
    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/chat-support")
    public String chatSupport(Model model) {
        model.addAttribute("pageTitle", "LIVE CHAT SUPPORT");
        model.addAttribute("currentPage", "chat-support");
        model.addAttribute("contentTemplate", "admin/chat-support/index");
        model.addAttribute("showSearch", false);
        return "admin/layouts/layout";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Lấy số đơn hàng đang chờ xử lý
            int pendingOrdersCount = orderService.getPendingOrders().size();
            
            // Lấy thống kê tổng quan
            DashboardStatsDTO stats = dashboardService.getDashboardStats();
            
            model.addAttribute("pendingOrdersCount", pendingOrdersCount);
            model.addAttribute("stats", stats);
            model.addAttribute("pageTitle", "DASHBOARD");
            model.addAttribute("currentPage", "dashboard");
            model.addAttribute("contentTemplate", "admin/dashboard/dashboard");
            model.addAttribute("showSearch", false);
            return "admin/layouts/layout";
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback với giá trị mặc định
            model.addAttribute("pendingOrdersCount", 0);
            model.addAttribute("stats", null);
            model.addAttribute("pageTitle", "DASHBOARD");
            model.addAttribute("currentPage", "dashboard");
            model.addAttribute("contentTemplate", "admin/dashboard/dashboard");
            model.addAttribute("showSearch", false);
            return "admin/layouts/layout";
        }
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        // Load statistics
        model.addAttribute("countPending", orderService.getPendingOrders().size());
        model.addAttribute("countConfirmed", orderService.getOrdersByStatus(OrderStatus.CONFIRMED).size());
        model.addAttribute("countShipping", orderService.getOrdersByStatus(OrderStatus.SHIPPING).size());
        model.addAttribute("countDelivered", orderService.getOrdersByStatus(OrderStatus.DELIVERED).size());
        
        model.addAttribute("pageTitle", "ORDERS");
        model.addAttribute("currentPage", "orders");
        model.addAttribute("contentTemplate", "admin/orders/orders");
        model.addAttribute("showSearch", true);
        return "admin/layouts/layout";
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public String customers(Model model) {
        model.addAttribute("pageTitle", "CUSTOMERS");
        model.addAttribute("currentPage", "customers");
        model.addAttribute("contentTemplate", "admin/customers/customers");
        model.addAttribute("showSearch", true);
        return "admin/layouts/layout";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("pageTitle", "CATEGORIES");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("contentTemplate", "admin/categories/categories");
        model.addAttribute("showSearch", true);
        return "admin/layouts/layout";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "SETTINGS");
        model.addAttribute("currentPage", "settings");
        model.addAttribute("contentTemplate", "admin/settings/settings");
        model.addAttribute("showSearch", false);
        return "admin/layouts/layout";
    }
    
    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("pageTitle", "QUẢN LÝ THÔNG BÁO");
        model.addAttribute("currentPage", "notifications");
        model.addAttribute("contentTemplate", "admin/notifications/index");
        model.addAttribute("showSearch", false);
        return "admin/layouts/layout";
    }
    
    @GetMapping("/ratings")
    public String ratings(Model model) {
        model.addAttribute("pageTitle", "QUẢN LÝ ĐÁNH GIÁ");
        model.addAttribute("currentPage", "ratings");
        model.addAttribute("contentTemplate", "admin/ratings/index");
        model.addAttribute("showSearch", true);
        return "admin/layouts/layout";
    }
}
