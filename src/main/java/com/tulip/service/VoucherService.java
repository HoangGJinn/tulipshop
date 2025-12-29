package com.tulip.service;

import com.tulip.entity.Voucher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VoucherService {
    List<Voucher> getAllVouchers();

    Optional<Voucher> getVoucherById(Long id);

    Optional<Voucher> getVoucherByCode(String code);

    Voucher saveVoucher(Voucher voucher);

    void deleteVoucher(Long id);

    // Validate and return discount amount. Returns 0 if invalid.
    BigDecimal calculateDiscount(String code, BigDecimal orderTotal);

    // Check if voucher is valid (exists, active, date, quantity, minSpend)
    boolean isValid(String code, BigDecimal orderTotal);
}
