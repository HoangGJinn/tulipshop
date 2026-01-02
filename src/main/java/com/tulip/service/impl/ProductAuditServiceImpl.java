package com.tulip.service.impl;

import com.tulip.dto.ProductAuditDTO;
import com.tulip.entity.product.ProductAudit;
import com.tulip.repository.ProductAuditRepository;
import com.tulip.service.ProductAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAuditServiceImpl implements ProductAuditService {

    private final ProductAuditRepository productAuditRepository;

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
