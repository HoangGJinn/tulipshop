package com.tulip.controller.admin;

import com.tulip.entity.Voucher;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
@RequestMapping("/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public String showVoucherList(Model model) {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        model.addAttribute("vouchers", vouchers);

        // Stats
        long totalVouchers = vouchers.size();
        long activeVouchers = vouchers.stream().filter(v -> Boolean.TRUE.equals(v.getStatus())).count();
        long expiredVouchers = vouchers.stream().filter(v -> !v.isValid()).count();
        int totalUsed = vouchers.stream().mapToInt(v -> v.getUsedCount() != null ? v.getUsedCount() : 0).sum();

        List<Map<String, Object>> stats = new ArrayList<>();

        Map<String, Object> stat1 = new HashMap<>();
        stat1.put("label", "Tổng voucher");
        stat1.put("value", totalVouchers);
        stat1.put("colorClass", "text-black");
        stat1.put("icon", "fas fa-ticket-alt");
        stats.add(stat1);

        Map<String, Object> stat2 = new HashMap<>();
        stat2.put("label", "Đang hoạt động");
        stat2.put("value", activeVouchers);
        stat2.put("colorClass", "text-green-500");
        stat2.put("icon", "fas fa-check-circle");
        stats.add(stat2);

        Map<String, Object> stat3 = new HashMap<>();
        stat3.put("label", "Hết hạn/Hết lượt");
        stat3.put("value", expiredVouchers);
        stat3.put("colorClass", "text-red-500");
        stat3.put("icon", "fas fa-times-circle");
        stats.add(stat3);

        Map<String, Object> stat4 = new HashMap<>();
        stat4.put("label", "Lượt sử dụng");
        stat4.put("value", totalUsed);
        stat4.put("colorClass", "text-blue-500");
        stat4.put("icon", "fas fa-chart-bar");
        stats.add(stat4);

        model.addAttribute("stats", stats);

        List<String> tableHeaders = List.of("Mã", "Loại", "Giá trị", "Đơn tối thiểu", "Số lượng", "Đã dùng", "Thời hạn",
                "Trạng thái", "Thao tác");
        model.addAttribute("tableHeaders", tableHeaders);

        model.addAttribute("pageTitle", "Quản lý Voucher");
        model.addAttribute("currentPage", "vouchers");
        model.addAttribute("contentTemplate", "admin/vouchers/list");
        model.addAttribute("showSearch", true);

        return "admin/layouts/layout";
    }

    @GetMapping("/form")
    public String showVoucherForm(@RequestParam(value = "id", required = false) Long id, Model model) {
        Voucher voucher;

        if (id != null) {
            voucher = voucherService.getVoucherById(id).orElse(new Voucher());
        } else {
            voucher = new Voucher();
            voucher.setStatus(true);
            voucher.setUsedCount(0);
        }

        model.addAttribute("voucher", voucher);
        model.addAttribute("isEditMode", id != null);
        model.addAttribute("discountTypes", Voucher.DiscountType.values());

        model.addAttribute("pageTitle", id != null ? "Cập nhật Voucher" : "Thêm Voucher mới");
        model.addAttribute("currentPage", "vouchers");
        model.addAttribute("contentTemplate", "admin/vouchers/form");

        return "admin/layouts/layout";
    }
}

