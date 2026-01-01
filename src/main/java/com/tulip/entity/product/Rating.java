package com.tulip.entity.product;

import com.tulip.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer stars;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Lưu thông tin variant dưới dạng chuỗi (VD: "Trắng, Size M")
    @Column(name = "variant_info")
    private String variantInfo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Điểm hữu ích của đánh giá (dùng để sắp xếp thông minh)
    @Column(name = "utility_score")
    @Builder.Default
    private Double utilityScore = 0.0;

    // Liên kết với đơn hàng để kiểm tra quyền đánh giá
    @Column(name = "order_id")
    private Long orderId;

    @OneToMany(mappedBy = "rating", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RatingImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}