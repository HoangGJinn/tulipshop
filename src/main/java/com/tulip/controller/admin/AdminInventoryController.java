package com.tulip.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminInventoryController {

    @GetMapping
    public String showInventoryPage(Model model) {
        model.addAttribute("pageTitle", "INVENTORY");
        model.addAttribute("currentPage", "inventory");
        model.addAttribute("contentTemplate", "admin/inventory/index");
        model.addAttribute("showSearch", true);
        return "admin/layouts/layout";
    }
}
