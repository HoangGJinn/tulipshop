package com.tulip.controller.admin;

import com.tulip.dto.response.CustomerDTO;
import com.tulip.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/customers")
public class CustomerManageController {

    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        List<CustomerDTO> customers = userService.getAllCustomers(null);

        // Count statistics
        long totalCustomers = customers.size();
        long activeCustomers = customers.stream().filter(CustomerDTO::getStatus).count();
        long verifiedCustomers = customers.stream().filter(c -> Boolean.TRUE.equals(c.getIsVerified())).count();

        model.addAttribute("customers", customers);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("activeCustomers", activeCustomers);
        model.addAttribute("verifiedCustomers", verifiedCustomers);
        model.addAttribute("pageTitle", "Quản lý Khách hàng");
        model.addAttribute("contentTemplate", "admin/customers/index");
        model.addAttribute("currentPage", "customers");
        return "admin/layouts/layout";
    }
}
