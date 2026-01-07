package com.tulip.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file) {
        try {
            // Upload file lên Cloudinary
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "tulip-fashion" // Tên thư mục trên Cloudinary
            ));

            // Trả về đường dẫn ảnh (URL)
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }
    
    /**
     * Tối ưu URL Cloudinary cho AI processing
     * Giảm kích thước ảnh xuống 512px để tiết kiệm tokens và tránh lỗi 429
     */
    public String optimizeImageForAI(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("cloudinary.com")) {
            return originalUrl;
        }
        
        // Chèn transformation vào URL Cloudinary để giảm kích thước
        // w_512: giới hạn chiều rộng 512px
        // c_limit: giữ tỷ lệ, không crop
        // q_auto: tự động tối ưu chất lượng
        // f_auto: tự động chọn format tốt nhất (webp, jpg, etc.)
        String transformation = "w_512,c_limit,q_auto,f_auto";
        
        if (originalUrl.contains("/upload/")) {
            return originalUrl.replace("/upload/", "/upload/" + transformation + "/");
        }
        
        return originalUrl;
    }
}