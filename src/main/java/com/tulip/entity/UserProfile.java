package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(length = 50)
    private String phone;

    @Column(length = 512)
    private String address;

    @Column(length = 512)
    private String avatar;

    @Column
    private Byte gender;

    @Column
    private LocalDate birthday;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ OneToMany với UserAddress
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<UserAddress> addresses = new ArrayList<>();

    public void setUser(User user) {
        this.user = user; // Bước 1: Profile nhận User này làm chủ sở hữu.

        // Bước 2: Kiểm tra điều kiện dừng (Guard Clause)
        // Nếu User tồn tại VÀ User đó chưa trỏ ngược lại Profile này...
        if (user != null && user.getProfile() != this) {
            user.setProfile(this); // Bước 3: ...thì bắt User phải trỏ lại Profile này.
        }
    }

    // Helper method để thêm địa chỉ
    public void addAddress(UserAddress address) {
        this.addresses.add(address);
        address.setProfile(this);
    }
}