package com.tulip.mapper;

import com.tulip.dto.UserAddressDTO;
import com.tulip.entity.UserAddress;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public UserAddressDTO toDTO(UserAddress address) {
        if (address == null) {
            return null;
        }

        // Logic an toàn: Nếu null thì trả về chuỗi rỗng ""
        return UserAddressDTO.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName() != null ? address.getRecipientName() : "")
                .recipientPhone(address.getRecipientPhone() != null ? address.getRecipientPhone() : "")
                .addressLine(address.getAddressLine() != null ? address.getAddressLine() : "")
                .village(address.getVillage() != null ? address.getVillage() : "")
                .district(address.getDistrict() != null ? address.getDistrict() : "")
                .province(address.getProvince() != null ? address.getProvince() : "")
                .isDefault(address.getIsDefault() != null ? address.getIsDefault() : false)
                // Gọi getFullAddress() an toàn
                .fullAddress(address.getFullAddress() != null ? address.getFullAddress() : "")
                .build();
    }

    public UserAddress toEntity(UserAddressDTO dto) {
        if (dto == null) {
            return null;
        }

        return UserAddress.builder()
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .addressLine(dto.getAddressLine())
                .village(dto.getVillage())
                .district(dto.getDistrict())
                .province(dto.getProvince())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .build();
    }

    public void updateEntityFromDTO(UserAddress address, UserAddressDTO dto) {
        if (address == null || dto == null) {
            return;
        }

        if (dto.getRecipientName() != null) address.setRecipientName(dto.getRecipientName());
        if (dto.getRecipientPhone() != null) address.setRecipientPhone(dto.getRecipientPhone());
        if (dto.getAddressLine() != null) address.setAddressLine(dto.getAddressLine());
        if (dto.getVillage() != null) address.setVillage(dto.getVillage());
        if (dto.getDistrict() != null) address.setDistrict(dto.getDistrict());
        if (dto.getProvince() != null) address.setProvince(dto.getProvince());
        if (dto.getIsDefault() != null) address.setIsDefault(dto.getIsDefault());
    }
}