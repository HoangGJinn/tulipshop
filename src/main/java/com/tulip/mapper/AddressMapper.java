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

        return UserAddressDTO.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .addressLine(address.getAddressLine())
                .village(address.getVillage())
                .district(address.getDistrict())
                .province(address.getProvince())
                .isDefault(address.getIsDefault())
                .fullAddress(address.getFullAddress())
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

        address.setRecipientName(dto.getRecipientName());
        address.setRecipientPhone(dto.getRecipientPhone());
        address.setAddressLine(dto.getAddressLine());
        address.setVillage(dto.getVillage());
        address.setDistrict(dto.getDistrict());
        address.setProvince(dto.getProvince());
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);
    }
}

