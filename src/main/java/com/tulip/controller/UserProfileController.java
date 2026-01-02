package com.tulip.controller;

import com.tulip.dto.UserProfileDTO;
import com.tulip.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.tulip.service.impl.CustomUserDetails;

import jakarta.validation.Valid;
import java.util.Map;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public String showProfile(Authentication authentication, Model model) {
        String email = authentication.getName();

        // Lấy thông tin profile dưới dạng DTO
        UserProfileDTO profileDTO = userService.getProfileByEmail(email);
        
        // Lấy thông tin về password để xác định form nào hiển thị
        Map<String, Object> passwordInfo = userService.getPasswordInfo(email);
        model.addAttribute("authProvider", passwordInfo.get("authProvider"));
        model.addAttribute("hasPassword", passwordInfo.get("hasPassword"));

        // Dùng DTO trực tiếp cho form
        model.addAttribute("form", profileDTO);
        model.addAttribute("profile", profileDTO);

        return "user/user-profile";
    }

    @PostMapping
    public String updateProfile(
            @Valid @ModelAttribute("form") UserProfileDTO form,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // Nếu có lỗi validation, load lại profile để hiển thị
            String email = authentication.getName();
            UserProfileDTO profileDTO = userService.getProfileByEmail(email);
            redirectAttributes.addFlashAttribute("profile", profileDTO);
            redirectAttributes.addFlashAttribute("form", form);
            return "user/user-profile";
        }

        try {
            String email = authentication.getName();
            userService.updateProfile(email, form, avatarFile);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/account";
    }
    private final com.tulip.service.AddressService addressService;

    // Hiển thị trang quản lý địa chỉ
    @GetMapping("/addresses")
    public String showAddresses(Authentication authentication, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();

        // Lấy danh sách địa chỉ và gửi sang View
        model.addAttribute("addresses", addressService.getUserAddresses(userId));
        return "user/user-addresses"; // Trả về file user/user-addresses.html
    }
}