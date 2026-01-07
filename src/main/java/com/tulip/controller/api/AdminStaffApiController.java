package com.tulip.controller.api;

import com.tulip.entity.Role;
import com.tulip.entity.User;
import com.tulip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffApiController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        List<User> allUsers = (keyword == null || keyword.trim().isEmpty())
                ? userRepository.findAllWithProfile()
                : userRepository.searchWithProfile(keyword.trim());

        // Lọc chỉ lấy users có role STAFF
        List<User> staffUsers = allUsers.stream()
                .filter(u -> u.getRole() == Role.STAFF)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = staffUsers.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("fullName", u.getProfile() != null ? u.getProfile().getFullName() : null);
            map.put("role", u.getRole() != null ? u.getRole().name() : null);
            map.put("status", u.getStatus());
            map.put("authProvider", u.getAuthProvider());
            map.put("createdAt", u.getCreatedAt());
            map.put("emailVerifiedAt", u.getEmailVerifiedAt());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean status = body.get("status") instanceof Boolean b ? b : null;
        if (status == null) return ResponseEntity.badRequest().body(Map.of("error", "Missing status"));

        return userRepository.findById(id).map(u -> {
            // Chỉ cho phép cập nhật status của STAFF
            if (u.getRole() != Role.STAFF) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ có thể cập nhật status của STAFF"));
            }
            u.setStatus(status);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("status", "ok"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String roleStr = body.get("role") != null ? body.get("role").toString() : null;
        if (roleStr == null || roleStr.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Missing role"));

        Role role = Role.fromString(roleStr);
        
        // Chỉ cho phép chuyển sang STAFF hoặc CUSTOMER (không cho chuyển sang ADMIN)
        if (role == Role.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể chuyển role sang ADMIN"));
        }

        return userRepository.findById(id).map(u -> {
            u.setRole(role);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("status", "ok"));
        }).orElse(ResponseEntity.notFound().build());
    }
}

