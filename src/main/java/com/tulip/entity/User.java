package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data // Các getter/setter, so sánh các đối tượng
@NoArgsConstructor // tạo constructor rỗng
@AllArgsConstructor // tạo constructor gồm các tham số
@Builder // khởi tạo đối tượng dễ dàng bằng .builder()
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @Column(name = "auth_provider", length = 50)
    @Builder.Default
    private String authProvider = "LOCAL"; // LOCAL, GOOGLE, FACEBOOK

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean status = true;

    @CreationTimestamp // Tự động lấy thời gian hiện tại gán vào khi bản ghi được Tạo mới lần đầu.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Tự động cập nhật thời gian hiện tại mỗi khi bản ghi bị Chỉnh sửa
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // 2 dòng này có tác dụng loại bỏ StackOverflowError khi User gọi toString của profile
    // profile xong profile gọi toString của User
    // Ý nghĩa: Loại bỏ field profile ra khỏi hàm toString() và equals().
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserProfile profile;

    public void setProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }
}
