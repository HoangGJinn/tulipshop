package com.tulip.service.impl;

import com.tulip.dto.UserAddressDTO;
import com.tulip.entity.User;
import com.tulip.entity.UserAddress;
import com.tulip.entity.UserProfile;
import com.tulip.mapper.AddressMapper;
import com.tulip.repository.UserAddressRepository;
import com.tulip.repository.UserProfileRepository;
import com.tulip.repository.UserRepository;
import com.tulip.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AddressMapper addressMapper;

    /**
     * Lấy profile từ userId, nếu chưa có thì tạo mới
     */
    private UserProfile getOrCreateProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder()
                    .user(user)
                    .build();
            user.setProfile(profile);
            userProfileRepository.save(profile);
        }
        return profile;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressDTO> getUserAddresses(Long userId) {
        UserProfile profile = getOrCreateProfile(userId);
        List<UserAddress> addresses = addressRepository.findByProfileIdOrderByIsDefaultDescIdDesc(profile.getId());
        return addresses.stream()
                .map(addressMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressDTO createAddress(Long userId, UserAddressDTO dto) {
        UserProfile profile = getOrCreateProfile(userId);

        UserAddress address = addressMapper.toEntity(dto);
        address.setProfile(profile);

        // Nếu đặt làm mặc định, bỏ đặt tất cả địa chỉ khác
        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            addressRepository.unsetAllDefaultAddresses(profile.getId());
        }

        // Nếu đây là địa chỉ đầu tiên, tự động đặt làm mặc định
        if (addressRepository.countByProfileId(profile.getId()) == 0) {
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        return addressMapper.toDTO(address);
    }

    @Override
    @Transactional
    public UserAddressDTO updateAddress(Long userId, Long addressId, UserAddressDTO dto) {
        UserProfile profile = getOrCreateProfile(userId);
        
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Address does not belong to user");
        }

        // Nếu đặt làm mặc định, bỏ đặt tất cả địa chỉ khác
        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            addressRepository.unsetAllDefaultAddresses(profile.getId());
        }

        addressMapper.updateEntityFromDTO(address, dto);
        address = addressRepository.save(address);
        return addressMapper.toDTO(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserProfile profile = getOrCreateProfile(userId);
        
        if (!addressRepository.existsByIdAndProfileId(addressId, profile.getId())) {
            throw new RuntimeException("Address not found or does not belong to user");
        }

        addressRepository.deleteById(addressId);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        UserProfile profile = getOrCreateProfile(userId);
        
        if (!addressRepository.existsByIdAndProfileId(addressId, profile.getId())) {
            throw new RuntimeException("Address not found or does not belong to user");
        }

        // Bỏ đặt tất cả địa chỉ mặc định
        addressRepository.unsetAllDefaultAddresses(profile.getId());

        // Đặt địa chỉ này làm mặc định
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        address.setIsDefault(true);
        addressRepository.save(address);
    }
}

