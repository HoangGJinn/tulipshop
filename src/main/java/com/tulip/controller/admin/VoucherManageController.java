package com.tulip.controller.admin;

import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/vouchers")
public class VoucherManageController {

    private final VoucherService voucherService;

    @GetMapping
    public String list(Model model) {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("pageTitle", "Quản lý Voucher");
        model.addAttribute("contentTemplate", "admin/vouchers/index");
        model.addAttribute("currentPage", "vouchers");
        return "admin/layouts/layout";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Voucher voucher,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            voucher.setCode(voucher.getCode().toUpperCase());
            voucherService.saveVoucher(voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo voucher thành công!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Voucher voucher,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            voucher.setId(id);
            voucher.setCode(voucher.getCode().toUpperCase());
            voucherService.saveVoucher(voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật voucher thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa voucher thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }
}
