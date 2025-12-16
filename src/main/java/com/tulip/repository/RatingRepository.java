package com.tulip.repository;

import com.tulip.entity.product.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByProductIdOrderByCreatedAtDesc(Long productId);


}
