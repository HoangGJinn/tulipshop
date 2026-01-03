package com.tulip.service.impl;

import com.tulip.entity.Voucher;
import com.tulip.repository.VoucherRepository;
import com.tulip.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Override
    public Optional<Voucher> getVoucherById(Long id) {
        return voucherRepository.findById(id);
    }

    @Override
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepository.findByCode(code != null ? code.toUpperCase() : null);
    }

    @Override
    public Voucher saveVoucher(Voucher voucher) {
        // Ensure code is uppercase
        if (voucher.getCode() != null) {
            voucher.setCode(voucher.getCode().toUpperCase());
        }

        // Initialize usedCount for new vouchers
        if (voucher.getId() == null && voucher.getUsedCount() == null) {
            voucher.setUsedCount(0);
        }

        return voucherRepository.save(voucher);
    }

    @Override
    public void deleteVoucher(Long id) {
        // Soft delete: set status to false
        voucherRepository.findById(id).ifPresent(v -> {
            v.setStatus(false);
            voucherRepository.save(v);
        });
    }

    @Override
    public boolean isValid(String code, BigDecimal orderTotal) {
        if (code == null)
            return false;
        Optional<Voucher> voucherOpt = voucherRepository.findByCode(code.toUpperCase());
        if (voucherOpt.isEmpty())
            return false;

        Voucher v = voucherOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // Check active status
        if (!Boolean.TRUE.equals(v.getStatus()))
            return false;

        // Check date range
        if (v.getStartAt() != null && now.isBefore(v.getStartAt()))
            return false;
        if (v.getExpireAt() != null && now.isAfter(v.getExpireAt()))
            return false;

        // Check min order value
        if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0)
            return false;

        return true;
    }

    @Override
    public BigDecimal calculateDiscount(String code, BigDecimal orderTotal) {
        if (!isValid(code, orderTotal))
            return BigDecimal.ZERO;

        Voucher v = voucherRepository.findByCode(code.toUpperCase()).get();
        BigDecimal discount = BigDecimal.ZERO;

        if (v.getType() == Voucher.DiscountType.AMOUNT) {
            discount = v.getDiscountValue();
        } else if (v.getType() == Voucher.DiscountType.PERCENT) {
            // Calculate percentage
            discount = orderTotal.multiply(v.getDiscountValue()).divide(new BigDecimal(100), RoundingMode.HALF_UP);
        }

        // Ensure discount doesn't exceed total order value (optional but good practice)
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }
}
