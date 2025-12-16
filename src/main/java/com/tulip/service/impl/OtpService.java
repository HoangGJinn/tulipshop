package com.tulip.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OtpService {

    private static final Integer EXPIRE_MINUTES = 5; // OTP hết hạn sau 5 phút
    private static final String DIGITS = "0123456789";
    
    private final Cache<String, String> otpCache = CacheBuilder.newBuilder()
            // Tự động xóa bản ghi sau 5 phút kể từ lúc ghi
            .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
            // Tối đa 1000 mã OTP
            .maximumSize(1000) 
            .build();

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp(String key) {
        StringBuilder otp = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            otp.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
        }
        
        String code = otp.toString();
        
        // Lưu vào Cache (Tự động đếm ngược 5 phút từ lúc thêm)
        otpCache.put(key, code);
        
        log.info("Generated OTP for {}: {}", key, code); // Chỉ bật log này khi dev/debug
        return code;
    }

    public boolean validateOtp(String key, String otpInput) {
        // Lấy mã OTP trong cache ra
        String cacheOtp = otpCache.getIfPresent(key);
        
        // 1. Nếu null -> Hết hạn hoặc chưa gửi
        if (cacheOtp == null) {
            return false;
        }
        
        // 2. Nếu khớp -> Xóa luôn để tránh dùng lại lần 2 (Replay Attack)
        if (cacheOtp.equals(otpInput)) {
            otpCache.invalidate(key); // Xóa ngay lập tức
            return true;
        }
        
        return false;
    }
    
    // Xóa thủ công (Dùng khi user yêu cầu gửi lại mã mới)
    public void clearOtp(String key) {
        otpCache.invalidate(key);
    }
}


