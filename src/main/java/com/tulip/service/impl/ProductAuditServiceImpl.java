package com.tulip.service.impl;

import com.tulip.dto.EditedProductDTO;
import com.tulip.dto.ProductAuditDTO;
import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductAudit;
import com.tulip.repository.ProductAuditRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.service.ProductAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAuditServiceImpl implements ProductAuditService {

    private final ProductAuditRepository productAuditRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductAuditDTO> getProductHistory(Long productId) {
        List<ProductAudit> audits = productAuditRepository.findByProductIdOrderByChangedAtDesc(productId);
        return audits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAuditDTO> getChangesByAdmin(String adminEmail) {
        List<ProductAudit> audits = productAuditRepository.findByChangedByOrderByChangedAtDesc(adminEmail);
        return audits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditedProductDTO> getAllEditedProducts() {
        // Lấy danh sách product IDs đã được chỉnh sửa
        List<Long> editedProductIds = productAuditRepository.findAllEditedProductIds();
        
        List<EditedProductDTO> result = new ArrayList<>();
        
        for (Long productId : editedProductIds) {
            // Lấy thông tin sản phẩm
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                continue; // Bỏ qua nếu sản phẩm đã bị xóa hoàn toàn
            }
            
            Product product = productOpt.get();
            
            // Lấy lịch sử chỉnh sửa cuối cùng
            List<ProductAudit> audits = productAuditRepository.findByProductIdOrderByChangedAtDesc(productId);
            if (audits.isEmpty()) {
                continue;
            }
            
            ProductAudit lastAudit = audits.get(0);
            
            // Đếm số lần chỉnh sửa
            Integer editCount = productAuditRepository.countByProductId(productId);
            
            // Tính tổng tồn kho
            int totalStock = product.getVariants().stream()
                    .flatMap(v -> v.getStocks().stream())
                    .mapToInt(s -> s.getQuantity())
                    .sum();
            
            // Tạo DTO
            EditedProductDTO dto = EditedProductDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(product.getThumbnail())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Chưa phân loại")
                    .currentPrice(product.getBasePrice())
                    .totalStock(totalStock)
                    .status(product.getStatus() != null ? product.getStatus().name() : "ACTIVE")
                    .lastChangeType(lastAudit.getChangeType())
                    .oldName(lastAudit.getOldName())
                    .newName(lastAudit.getNewName())
                    .oldPrice(lastAudit.getOldPrice())
                    .newPrice(lastAudit.getNewPrice())
                    .changedBy(lastAudit.getChangedBy())
                    .changedAt(lastAudit.getChangedAt())
                    .editCount(editCount)
                    .build();
            
            result.add(dto);
        }
        
        // Sắp xếp theo thời gian chỉnh sửa gần nhất (mới nhất lên đầu)
        result.sort((a, b) -> b.getChangedAt().compareTo(a.getChangedAt()));
        
        return result;
    }

    private ProductAuditDTO convertToDTO(ProductAudit audit) {
        return ProductAuditDTO.builder()
                .id(audit.getId())
                .productId(audit.getProductId())
                .oldName(audit.getOldName())
                .newName(audit.getNewName())
                .oldPrice(audit.getOldPrice())
                .newPrice(audit.getNewPrice())
                .changedBy(audit.getChangedBy())
                .changedAt(audit.getChangedAt())
                .changeType(audit.getChangeType())
                .build();
    }
}
