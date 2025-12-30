package com.tulip.controller.admin;

import com.tulip.entity.enums.OrderStatus;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminViewController {

    private final OrderService orderService;

    // Xử lý route /admin - redirect đến dashboard
    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "DASHBOARD");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("showSearch", false);
        return "admin/dashboard/dashboard";
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
        model.addAttribute("showSearch", true);
        return "admin/orders/orders";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("pageTitle", "CUSTOMERS");
        model.addAttribute("currentPage", "customers");
        model.addAttribute("showSearch", true);
        return "admin/customers/customers";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("pageTitle", "CATEGORIES");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("showSearch", true);
        return "admin/categories/categories";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "SETTINGS");
        model.addAttribute("currentPage", "settings");
        model.addAttribute("showSearch", false);
        return "admin/settings/settings";
    }
}
