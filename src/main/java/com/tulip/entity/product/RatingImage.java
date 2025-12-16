package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rating_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rating_id")
    @ToString.Exclude
    private Rating rating;

    private String imageUrl;
}