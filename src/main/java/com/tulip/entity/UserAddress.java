package com.tulip.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserProfile profile;

    @Column(name = "recipient_name", length = 255, nullable = false)
    private String recipientName; // Tên người nhận

    @Column(name = "recipient_phone", length = 50, nullable = false)
    private String recipientPhone; // Số điện thoại người nhận

    @Column(name = "address_line", length = 512, nullable = false)
    private String addressLine; // Số nhà, tên đường

    @Column(length = 100)
    private String village; // Phường/Xã

    @Column(length = 100)
    private String district; // Quận/Huyện

    @Column(length = 100)
    private String province; // Tỉnh/Thành phố

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false; // Địa chỉ mặc định


    // Helper method để lấy địa chỉ đầy đủ
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null) sb.append(addressLine);
        if (village != null && !village.trim().isEmpty()) sb.append(", ").append(village);
        if (district != null && !district.trim().isEmpty()) sb.append(", ").append(district);
        if (province != null && !province.trim().isEmpty()) sb.append(", ").append(province);
        return sb.toString();
    }
}

