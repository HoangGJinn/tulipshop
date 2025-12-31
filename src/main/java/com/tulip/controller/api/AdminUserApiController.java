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

@RestController
@RequestMapping("/v1/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApiController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        List<User> users = (keyword == null || keyword.trim().isEmpty())
                ? userRepository.findAllWithProfile()
                : userRepository.searchWithProfile(keyword.trim());

        List<Map<String, Object>> result = users.stream().map(u -> {
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

        return userRepository.findById(id).map(u -> {
            u.setRole(role);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("status", "ok"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
