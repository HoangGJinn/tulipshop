package com.tulip.dto.response;

import com.tulip.entity.Role;
import com.tulip.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private String role;
    private Boolean status;
    private String authProvider;
    private String createdAt;
    private String emailVerifiedAt;
    private Integer totalOrders;
    private Boolean isVerified;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static CustomerDTO fromEntity(User user) {
        if (user == null)
            return null;

        String fullName = "";
        String phone = "";
        String avatar = null;

        if (user.getProfile() != null) {
            fullName = user.getProfile().getFullName() != null ? user.getProfile().getFullName() : "";
            phone = user.getProfile().getPhone() != null ? user.getProfile().getPhone() : "";
            avatar = user.getProfile().getAvatar();
        }

        return CustomerDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(fullName)
                .phone(phone)
                .avatar(avatar)
                .role(user.getRole() != null ? user.getRole().name() : Role.CUSTOMER.name())
                .status(Boolean.TRUE.equals(user.getStatus()))
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider() : "LOCAL")
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(FORMATTER) : null)
                .emailVerifiedAt(user.getEmailVerifiedAt() != null ? user.getEmailVerifiedAt().format(FORMATTER) : null)
                .isVerified(user.getEmailVerifiedAt() != null)
                .totalOrders(0) // Will be set separately if needed
                .build();
    }
}
