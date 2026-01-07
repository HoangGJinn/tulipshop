package com.tulip.service;

import com.tulip.dto.response.UserVoucherDTO;
import com.tulip.entity.User;
import com.tulip.entity.UserVoucher;
import com.tulip.entity.Voucher;

import java.util.List;

public interface UserVoucherService {

    /**
     * Lấy danh sách voucher khả dụng của user (chưa dùng, còn hạn)
     */
    List<UserVoucherDTO> getAvailableVouchers(Long userId);

    /**
     * Lấy tất cả voucher của user (bao gồm đã dùng)
     */
    List<UserVoucherDTO> getAllVouchers(Long userId);

    /**
     * Tặng voucher cho user khi đánh giá sản phẩm
     * 
     * @return UserVoucher nếu tặng thành công, null nếu không tìm thấy voucher phù
     *         hợp
     */
    UserVoucher grantRatingVoucher(User user, Long ratingId);

    /**
     * Tặng voucher sự kiện cho user
     */
    UserVoucher grantEventVoucher(User user, Voucher voucher, String eventName);

    /**
     * Đánh dấu voucher đã sử dụng
     */
    void markAsUsed(Long userId, Long voucherId);

    /**
     * Kiểm tra user có voucher này và còn dùng được không
     */
    boolean canUseVoucher(Long userId, String voucherCode);
}
