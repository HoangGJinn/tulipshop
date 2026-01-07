package com.tulip.controller.api;

import com.tulip.dto.EditedProductDTO;
import com.tulip.dto.ProductAuditDTO;
import com.tulip.service.ProductAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product-audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ProductAuditApiController {

    private final ProductAuditService productAuditService;

    /**
     * Lấy lịch sử thay đổi của một sản phẩm
     * GET /api/admin/product-audit/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductAuditDTO>> getProductHistory(@PathVariable Long productId) {
        List<ProductAuditDTO> history = productAuditService.getProductHistory(productId);
        return ResponseEntity.ok(history);
    }

    /**
     * Lấy lịch sử thay đổi do một admin thực hiện
     * GET /api/admin/product-audit/admin/{adminEmail}
     */
    @GetMapping("/admin/{adminEmail}")
    public ResponseEntity<List<ProductAuditDTO>> getChangesByAdmin(@PathVariable String adminEmail) {
        List<ProductAuditDTO> changes = productAuditService.getChangesByAdmin(adminEmail);
        return ResponseEntity.ok(changes);
    }

    /**
     * Lấy danh sách tất cả sản phẩm đã được chỉnh sửa
     * GET /api/admin/product-audit/edited-products
     */
    @GetMapping("/edited-products")
    public ResponseEntity<List<EditedProductDTO>> getAllEditedProducts() {
        List<EditedProductDTO> editedProducts = productAuditService.getAllEditedProducts();
        return ResponseEntity.ok(editedProducts);
    }
}
