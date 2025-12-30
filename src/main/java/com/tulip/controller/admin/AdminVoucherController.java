package com.tulip.controller.admin;

import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    // --- VIEW ---
    @GetMapping("/admin/vouchers")
    public String showVoucherList(Model model) {
        // Stats
        List<Voucher> vouchers = voucherService.getAllVouchers();
        long activeCount = vouchers.stream().filter(v -> Boolean.TRUE.equals(v.getStatus())).count();
        long totalCount = vouchers.size();

        model.addAttribute("pageTitle", "Quản lý Voucher");
        model.addAttribute("currentPage", "vouchers");
        model.addAttribute("contentTemplate", "admin/voucher/list");
        model.addAttribute("tableHeaders",
                List.of("Mã", "Loại", "Giá trị", "Điều kiện", "Hạn dùng", "Trạng thái", "Thao tác"));

        // Pass stats simply
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("totalCount", totalCount);

        return "admin/layouts/layout";
    }

    @GetMapping("/admin/vouchers/form")
    public String showVoucherForm(@RequestParam(required = false) Long id, Model model) {
        Voucher voucher = new Voucher();
        if (id != null) {
            voucher = voucherService.getVoucherById(id).orElse(new Voucher());
        } else {
            voucher.setStatus(true); // Default active
        }

        model.addAttribute("voucher", voucher);
        model.addAttribute("pageTitle", id != null ? "Cập nhật Voucher" : "Tạo Voucher Mới");
        model.addAttribute("currentPage", "vouchers");
        model.addAttribute("contentTemplate", "admin/voucher/form");
        return "admin/layouts/layout";
    }

}
