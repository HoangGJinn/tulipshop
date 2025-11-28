package com.tulip.controller;

import com.tulip.service.UserService;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    // Tưởng tượng model là 1 cái đĩa, mình bỏ dữ liệu vào đĩa và mang tới view
    // Rồi view (thằng thymeleaf) sẽ lấy dữ liệu từ đĩa ra dùng
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "success", required = false) String success,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng!");
        }
        if (logout != null) {
            model.addAttribute("message", "Bạn đã đăng xuất thành công.");
        }
        if (success != null) {
            model.addAttribute("message", "Đăng ký thành công. Vui lòng đăng nhập để tiếp tục.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br, Model model, RedirectAttributes redirectAttributes) {
        if (br.hasErrors()) {
            return "register";
        }
        try {
            userService.register(
                    form.getEmail(),
                    form.getPassword(),
                    form.getFullName(),
                    form.getPhone()
            );
            return "redirect:/login";
        } catch (Exception ex){
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @Data
    @NoArgsConstructor
    public static class RegisterForm {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;
        
        @NotBlank(message = "Mật khẩu không được để trống")
        private String password;

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        private String confirmPassword;

        @NotBlank(message = "Họ và tên không được để trống")
        private String fullName;

        private String phone;

        @AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp")
        public boolean isPasswordMatch() {
            if (password == null || confirmPassword == null) {
                return false;
            }
            return password.equals(confirmPassword);
        }

    }
}
