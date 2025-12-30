package com.tulip.service;

import com.tulip.dto.VoucherApplyRequestDTO;
import com.tulip.dto.VoucherApplyResponseDTO;
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

    /**
     * Kiểm tra voucher có hợp lệ để sử dụng không
     */
    boolean isVoucherValid(String code, BigDecimal orderValue);

    /**
     * Tính giá trị giảm giá
     */
    BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderValue);

    /**
     * Sử dụng voucher (tăng usedCount)
     */
    void useVoucher(String code);

    /**
     * Lấy danh sách voucher có thể áp dụng cho đơn hàng
     */
    List<Voucher> getApplicableVouchers(BigDecimal orderTotal);

    /**
     * Tính toán và áp dụng voucher
     */
    VoucherApplyResponseDTO calculateDiscount(VoucherApplyRequestDTO request);
}
