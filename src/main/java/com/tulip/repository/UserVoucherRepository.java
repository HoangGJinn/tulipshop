package com.tulip.repository;

import com.tulip.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    // Lấy tất cả voucher của user (chưa sử dụng và còn hạn)
    @Query("SELECT uv FROM UserVoucher uv " +
            "JOIN FETCH uv.voucher v " +
            "WHERE uv.user.id = :userId " +
            "AND uv.isUsed = false " +
            "AND (uv.expireAt IS NULL OR uv.expireAt > CURRENT_TIMESTAMP) " +
            "AND v.status = true " +
            "ORDER BY uv.receivedAt DESC")
    List<UserVoucher> findAvailableByUserId(@Param("userId") Long userId);

    // Lấy tất cả voucher của user (bao gồm đã dùng)
    @Query("SELECT uv FROM UserVoucher uv " +
            "JOIN FETCH uv.voucher v " +
            "WHERE uv.user.id = :userId " +
            "ORDER BY uv.receivedAt DESC")
    List<UserVoucher> findAllByUserId(@Param("userId") Long userId);

    // Kiểm tra user đã có voucher này chưa
    @Query("SELECT uv FROM UserVoucher uv " +
            "WHERE uv.user.id = :userId AND uv.voucher.id = :voucherId")
    Optional<UserVoucher> findByUserIdAndVoucherId(@Param("userId") Long userId,
            @Param("voucherId") Long voucherId);

    // Kiểm tra user đã nhận voucher từ rating này chưa
    @Query("SELECT uv FROM UserVoucher uv " +
            "WHERE uv.user.id = :userId " +
            "AND uv.source = 'RATING' " +
            "AND uv.sourceId = :ratingId")
    Optional<UserVoucher> findByUserIdAndRatingId(@Param("userId") Long userId,
            @Param("ratingId") Long ratingId);

    // Đếm số voucher user đã nhận từ đánh giá
    @Query("SELECT COUNT(uv) FROM UserVoucher uv " +
            "WHERE uv.user.id = :userId AND uv.source = 'RATING'")
    long countRatingVouchersByUserId(@Param("userId") Long userId);
}
