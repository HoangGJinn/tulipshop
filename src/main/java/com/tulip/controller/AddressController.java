package com.tulip.controller;

import com.tulip.dto.UserAddressDTO;
import com.tulip.service.AddressService;
import com.tulip.service.impl.CustomUserDetails;
import com.tulip.service.impl.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    
    private final AddressService addressService;
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUserId();
        }
        
        return null;
    }
    
    @GetMapping
    public ResponseEntity<List<UserAddressDTO>> getUserAddresses(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<UserAddressDTO> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }
    
    @PostMapping
    public ResponseEntity<?> createAddress(
            @Valid @RequestBody UserAddressDTO dto,
            BindingResult bindingResult,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập");
        }
        
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        
        try {
            UserAddressDTO address = addressService.createAddress(userId, dto);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressDTO dto,
            BindingResult bindingResult,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        
        try {
            UserAddressDTO address = addressService.updateAddress(userId, addressId, dto);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            addressService.deleteAddress(userId, addressId);
            return ResponseEntity.ok("Đã xóa địa chỉ");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/{addressId}/set-default")
    public ResponseEntity<?> setDefaultAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            addressService.setDefaultAddress(userId, addressId);
            return ResponseEntity.ok("Đã đặt làm địa chỉ mặc định");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

