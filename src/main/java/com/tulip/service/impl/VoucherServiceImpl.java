package com.tulip.service.impl;

import com.tulip.dto.VoucherApplyRequestDTO;
import com.tulip.dto.VoucherApplyResponseDTO;
import com.tulip.dto.VoucherDTO;
import com.tulip.entity.Voucher;
import com.tulip.repository.VoucherRepository;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> getVoucherById(Long id) {
        return voucherRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    @Override
    public Voucher saveVoucher(Voucher voucher) {
        if (voucher.getUsedCount() == null) {
            voucher.setUsedCount(0);
        }
        return voucherRepository.save(voucher);
    }

    @Override
    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVoucherValid(String code, BigDecimal orderValue) {
        Optional<Voucher> voucherOpt = voucherRepository.findByCode(code);
        if (voucherOpt.isEmpty()) {
            return false;
        }

        Voucher voucher = voucherOpt.get();

        // Kiểm tra voucher còn hiệu lực
        if (!voucher.isValid()) {
            return false;
        }

        // Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getMinOrderValue() != null && orderValue.compareTo(voucher.getMinOrderValue()) < 0) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderValue) {
        if (voucher == null || voucher.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

        if (voucher.getType() == Voucher.DiscountType.PERCENT) {
            // Giảm theo phần trăm
            BigDecimal discount = orderValue.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            // Đảm bảo không giảm quá tổng đơn hàng
            return discount.compareTo(orderValue) > 0 ? orderValue : discount;
        } else {
            // Giảm trực tiếp theo số tiền
            // Đảm bảo không giảm quá tổng đơn hàng
            return voucher.getDiscountValue().compareTo(orderValue) > 0 
                    ? orderValue 
                    : voucher.getDiscountValue();
        }
    }

    @Override
    public void useVoucher(String code) {
        voucherRepository.findByCode(code).ifPresent(voucher -> {
            if (voucher.getUsedCount() == null) {
                voucher.setUsedCount(0);
            }
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getApplicableVouchers(BigDecimal orderTotal) {
        List<Voucher> allVouchers = voucherRepository.findAll();
        
        return allVouchers.stream()
                .filter(v -> {
                    // Kiểm tra status
                    if (!Boolean.TRUE.equals(v.getStatus())) {
                        return false;
                    }
                    
                    // Kiểm tra số lượng còn lại
                    if (v.getQuantity() == null || v.getUsedCount() == null) {
                        return false;
                    }
                    if (v.getQuantity() <= v.getUsedCount()) {
                        return false;
                    }
                    
                    // Kiểm tra thời gian
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (v.getStartAt() != null && now.isBefore(v.getStartAt())) {
                        return false;
                    }
                    if (v.getExpireAt() != null && now.isAfter(v.getExpireAt())) {
                        return false;
                    }
                    
                    // Kiểm tra đơn hàng tối thiểu
                    if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherApplyResponseDTO calculateDiscount(VoucherApplyRequestDTO request) {
        Optional<Voucher> voucherOpt = voucherRepository.findByCode(request.getCode());
        
        if (voucherOpt.isEmpty()) {
            return VoucherApplyResponseDTO.builder()
                    .success(false)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(request.getOrderTotal())
                    .message("Mã voucher không tồn tại")
                    .build();
        }
        
        Voucher voucher = voucherOpt.get();
        
        // Kiểm tra voucher có hợp lệ không
        if (!isVoucherValid(request.getCode(), request.getOrderTotal())) {
            String message = "Mã voucher không hợp lệ";
            if (voucher.getMinOrderValue() != null && 
                request.getOrderTotal().compareTo(voucher.getMinOrderValue()) < 0) {
                message = "Đơn hàng tối thiểu phải từ " + 
                          voucher.getMinOrderValue() + "₫ để sử dụng mã này";
            } else if (!voucher.isValid()) {
                message = "Mã voucher đã hết hạn hoặc hết lượt sử dụng";
            }
            
            return VoucherApplyResponseDTO.builder()
                    .success(false)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(request.getOrderTotal())
                    .message(message)
                    .build();
        }
        
        // Tính toán giảm giá
        BigDecimal discountAmount = calculateDiscount(voucher, request.getOrderTotal());
        BigDecimal finalAmount = request.getOrderTotal().subtract(discountAmount);
        
        // Tạo VoucherDTO để trả về
        VoucherDTO voucherDTO = VoucherDTO.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .type(voucher.getType().name())
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .build();
        
        return VoucherApplyResponseDTO.builder()
                .success(true)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .message("Áp dụng mã giảm giá thành công")
                .voucher(voucherDTO)
                .build();
    }
}
