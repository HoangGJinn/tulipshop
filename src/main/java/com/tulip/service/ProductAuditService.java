package com.tulip.service;

import com.tulip.dto.EditedProductDTO;
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
    
    /**
     * Lấy danh sách tất cả các sản phẩm đã được chỉnh sửa
     * Kèm theo thông tin chỉnh sửa cuối cùng và số lần chỉnh sửa
     */
    List<EditedProductDTO> getAllEditedProducts();
}
