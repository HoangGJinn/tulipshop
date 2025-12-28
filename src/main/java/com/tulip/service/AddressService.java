package com.tulip.service;

import com.tulip.dto.UserAddressDTO;

import java.util.List;

public interface AddressService {
    List<UserAddressDTO> getUserAddresses(Long userId);
    UserAddressDTO getUserAddressById(Long addressId);
    UserAddressDTO createAddress(Long userId, UserAddressDTO dto);
    UserAddressDTO updateAddress(Long userId, Long addressId, UserAddressDTO dto);
    void deleteAddress(Long userId, Long addressId);
    void setDefaultAddress(Long userId, Long addressId);
}

