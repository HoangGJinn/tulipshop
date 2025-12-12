package com.tulip.service;

import com.tulip.dto.RatingDTO;
import com.tulip.dto.RatingSummaryDTO;

import java.util.List;

public interface RatingService{
    List<RatingDTO> getRatingsByProduct(Long productId);
    RatingSummaryDTO getRatingSummary(Long productId);

}
