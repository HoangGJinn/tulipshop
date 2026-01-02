package com.tulip.service;

import com.tulip.dto.ProductAuditDTO;
import java.util.List;

public interface ProductAuditService {
    /**
     * Lấy lịch sử thay đổi của một sản phẩm
     */
    List<ProductAuditDTO> getProductHistory(Long productId);
    
    /**
     * Lấy lịch sử thay đổi do một admin thực hiện
     */
    List<ProductAuditDTO> getChangesByAdmin(String adminEmail);
}
