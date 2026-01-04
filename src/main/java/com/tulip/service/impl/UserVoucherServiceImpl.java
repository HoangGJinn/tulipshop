package com.tulip.service.impl;

import com.tulip.dto.response.UserVoucherDTO;
import com.tulip.entity.User;
import com.tulip.entity.UserVoucher;
import com.tulip.entity.Voucher;
import com.tulip.repository.UserVoucherRepository;
import com.tulip.repository.VoucherRepository;
import com.tulip.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVoucherServiceImpl implements UserVoucherService {

    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRepository voucherRepository;

    // Cấu hình voucher thưởng đánh giá (có thể đặt trong application.properties)
    @Value("${voucher.rating.discount-percent:5}")
    private int ratingVoucherDiscountPercent;

    @Value("${voucher.rating.expire-days:30}")
    private int ratingVoucherExpireDays;

    @Value("${voucher.rating.min-order:100000}")
    private long ratingVoucherMinOrder;

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucherDTO> getAvailableVouchers(Long userId) {
        return userVoucherRepository.findAvailableByUserId(userId).stream()
                .map(UserVoucherDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucherDTO> getAllVouchers(Long userId) {
        return userVoucherRepository.findAllByUserId(userId).stream()
                .map(UserVoucherDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserVoucher grantRatingVoucher(User user, Long ratingId) {
        // Kiểm tra đã nhận voucher cho rating này chưa
        Optional<UserVoucher> existing = userVoucherRepository.findByUserIdAndRatingId(user.getId(), ratingId);
        if (existing.isPresent()) {
            log.info("User {} đã nhận voucher cho rating {}", user.getId(), ratingId);
            return existing.get();
        }

        // Tìm hoặc tạo voucher template cho đánh giá
        Voucher ratingVoucher = findOrCreateRatingVoucher();
        if (ratingVoucher == null) {
            log.warn("Không tìm thấy voucher template cho đánh giá");
            return null;
        }

        // Tạo UserVoucher
        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(ratingVoucher)
                .isUsed(false)
                .source("RATING")
                .sourceId(ratingId)
                .expireAt(LocalDateTime.now().plusDays(ratingVoucherExpireDays))
                .build();

        UserVoucher saved = userVoucherRepository.save(userVoucher);
        log.info("✅ Đã tặng voucher {} cho user {} vì đánh giá sản phẩm (ratingId={})",
                ratingVoucher.getCode(), user.getId(), ratingId);

        return saved;
    }

    @Override
    @Transactional
    public UserVoucher grantEventVoucher(User user, Voucher voucher, String eventName) {
        // Kiểm tra đã có voucher này chưa
        Optional<UserVoucher> existing = userVoucherRepository.findByUserIdAndVoucherId(user.getId(), voucher.getId());
        if (existing.isPresent()) {
            log.info("User {} đã có voucher {}", user.getId(), voucher.getCode());
            return existing.get();
        }

        // Tạo UserVoucher
        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .isUsed(false)
                .source("EVENT")
                .expireAt(voucher.getExpireAt())
                .build();

        UserVoucher saved = userVoucherRepository.save(userVoucher);
        log.info("✅ Đã tặng voucher {} cho user {} (sự kiện: {})",
                voucher.getCode(), user.getId(), eventName);

        return saved;
    }

    @Override
    @Transactional
    public void markAsUsed(Long userId, Long voucherId) {
        Optional<UserVoucher> uv = userVoucherRepository.findByUserIdAndVoucherId(userId, voucherId);
        if (uv.isPresent()) {
            UserVoucher userVoucher = uv.get();
            userVoucher.setIsUsed(true);
            userVoucher.setUsedAt(LocalDateTime.now());
            userVoucherRepository.save(userVoucher);
            log.info("User {} đã sử dụng voucher {}", userId, voucherId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUseVoucher(Long userId, String voucherCode) {
        // Kiểm tra voucher công khai hoặc voucher riêng của user
        Optional<Voucher> voucher = voucherRepository.findByCode(voucherCode);
        if (voucher.isEmpty()) {
            return false;
        }

        Voucher v = voucher.get();

        // Nếu là voucher công khai, kiểm tra điều kiện chung
        if (Boolean.TRUE.equals(v.getIsPublic())) {
            return v.isValid(BigDecimal.ZERO);
        }

        // Nếu là voucher riêng, kiểm tra user có voucher này không
        Optional<UserVoucher> uv = userVoucherRepository.findByUserIdAndVoucherId(userId, v.getId());
        if (uv.isEmpty()) {
            return false;
        }

        UserVoucher userVoucher = uv.get();
        if (Boolean.TRUE.equals(userVoucher.getIsUsed())) {
            return false;
        }

        // Kiểm tra thời hạn riêng của user
        if (userVoucher.getExpireAt() != null && LocalDateTime.now().isAfter(userVoucher.getExpireAt())) {
            return false;
        }

        return true;
    }

    /**
     * Tìm hoặc tạo voucher template cho việc tặng khi đánh giá
     */
    private Voucher findOrCreateRatingVoucher() {
        // Tìm voucher có code bắt đầu bằng "RATING_REWARD" và còn hoạt động
        String ratingVoucherCode = "RATING_REWARD";
        Optional<Voucher> existing = voucherRepository.findByCode(ratingVoucherCode);

        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getStatus())) {
            return existing.get();
        }

        // Nếu chưa có, tạo mới
        Voucher ratingVoucher = Voucher.builder()
                .code(ratingVoucherCode)
                .name("Quà tặng đánh giá sản phẩm")
                .description(
                        "Voucher giảm giá dành cho khách hàng đã đánh giá sản phẩm. Cảm ơn bạn đã chia sẻ trải nghiệm!")
                .type(Voucher.DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(ratingVoucherDiscountPercent))
                .minOrderValue(BigDecimal.valueOf(ratingVoucherMinOrder))
                .quantity(null) // Không giới hạn
                .usedCount(0)
                .status(true)
                .isPublic(false) // Chỉ phát riêng cho user
                .build();

        Voucher saved = voucherRepository.save(ratingVoucher);
        log.info("✅ Đã tạo voucher template cho đánh giá: {}", ratingVoucherCode);

        return saved;
    }
}
