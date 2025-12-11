package com.tulip.mapper;

import com.tulip.dto.UserProfileDTO;
import com.tulip.entity.User;
import com.tulip.entity.UserProfile;
import com.tulip.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {
    
    private final AddressService addressService; // Inject để lấy danh sách địa chỉ

    /**
     * Convert User + UserProfile Entity sang UserProfileDTO
     */
    public UserProfileDTO toDTO(User user, UserProfile profile) {
        if (user == null) {
            return null;
        }

        var addresses = addressService.getUserAddresses(user.getId());

        return UserProfileDTO.builder()
                .email(user.getEmail())
                .fullName(profile != null ? profile.getFullName() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .address(profile != null ? profile.getAddress() : null) // Giữ lại để backward compatibility
                .avatar(profile != null ? profile.getAvatar() : null)
                .gender(profile != null ? profile.getGender() : null)
                .birthday(profile != null ? profile.getBirthday() : null)
                .addresses(addresses)
                .build();
    }

    /**
     * Convert User Entity sang UserProfileDTO (khi chưa có profile)
     */
    public UserProfileDTO toDTO(User user) {
        return toDTO(user, user.getProfile());
    }

    /**
     * Update UserProfile Entity từ UserProfileDTO
     * Không tạo entity mới, chỉ update các field
     */
    public void updateEntityFromDTO(UserProfile profile, UserProfileDTO dto) {
        if (profile == null || dto == null) {
            return;
        }

        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone() != null && !dto.getPhone().trim().isEmpty() ? dto.getPhone() : null);
        profile.setAddress(dto.getAddress() != null && !dto.getAddress().trim().isEmpty() ? dto.getAddress() : null);
        profile.setGender(dto.getGender());
        profile.setBirthday(dto.getBirthday());
    }
}