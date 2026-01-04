package com.tulip.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    @GetMapping
    public String staff(Model model) {
        model.addAttribute("pageTitle", "STAFFS");
        model.addAttribute("currentPage", "staff");
        model.addAttribute("contentTemplate", "admin/staff/staff");
        model.addAttribute("showSearch", true);
        
        // Truyền tableHeaders từ controller
        List<String> tableHeaders = Arrays.asList("Email", "Họ tên", "Role", "Trạng thái", "Provider", "Thao tác");
        model.addAttribute("tableHeaders", tableHeaders);
        
        return "admin/layouts/layout";
    }
}

