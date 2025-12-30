package com.tulip.config;

import com.tulip.entity.Role;
import com.tulip.entity.User;
import com.tulip.entity.UserProfile;
import com.tulip.entity.product.*;
import com.tulip.repository.CategoryRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.repository.SizeRepository;
import com.tulip.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer {
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final SizeRepository sizeRepository;

        private final JdbcTemplate jdbcTemplate;

        @PostConstruct
        public void init() {
                initChatTables();

                if (!userRepository.existsByEmail("admin@local")) {
                        User admin = User.builder()
                                        .email("admin@local")
                                        .passwordHash(passwordEncoder.encode("admin123"))
                                        .authProvider("LOCAL")
                                        .role(Role.ADMIN)
                                        .status(true)
                                        .emailVerifiedAt(LocalDateTime.now())
                                        .build();
                        UserProfile p = UserProfile.builder()
                                        .fullName("Administrator")
                                        .build();
                        admin.setProfile(p);
                        userRepository.save(admin);
                }
                initProducts();
        }

        private void initChatTables() {
                try {
                        // Chat Rooms Table
                        String createChatRoomTable = """
                                            CREATE TABLE IF NOT EXISTS chat_rooms (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                customer_id BIGINT NOT NULL,
                                                staff_id BIGINT,
                                                status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
                                                last_message_at DATETIME,
                                                customer_last_seen_at DATETIME,
                                                staff_last_seen_at DATETIME,
                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
                                                FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE SET NULL,
                                                INDEX idx_customer_id (customer_id),
                                                INDEX idx_staff_id (staff_id),
                                                INDEX idx_status (status)
                                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                                        """;
                        jdbcTemplate.execute(createChatRoomTable);

                        // Chat Messages Table
                        String createChatMessageTable = """
                                            CREATE TABLE IF NOT EXISTS chat_messages (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                chat_room_id BIGINT NOT NULL,
                                                sender_id BIGINT NOT NULL,
                                                type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
                                                content TEXT,
                                                seen BOOLEAN DEFAULT FALSE,
                                                seen_at DATETIME,
                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
                                                FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
                                                INDEX idx_chat_room_id (chat_room_id),
                                                INDEX idx_sender_id (sender_id),
                                                INDEX idx_created_at (created_at)
                                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                                        """;
                        jdbcTemplate.execute(createChatMessageTable);

                        System.out.println("✅ Đã khởi tạo bảng chat_rooms và chat_messages thành công!");
                } catch (Exception e) {
                        System.err.println("⚠️ Lỗi khi khởi tạo bảng chat: " + e.getMessage());
                }
        }

        private void initProducts() {
                if (productRepository.count() > 0)
                        return; // Đã có dữ liệu thì thôi

                // 1. Tạo Sizes
                Size s = sizeRepository.save(Size.builder().code("S").sortOrder(1).build());
                Size m = sizeRepository.save(Size.builder().code("M").sortOrder(2).build());
                Size l = sizeRepository.save(Size.builder().code("L").sortOrder(3).build());
                Size xl = sizeRepository.save(Size.builder().code("XL").sortOrder(4).build());

                // 2. Tạo Category
                Category aoKieu = categoryRepository.save(Category.builder().name("Áo Kiểu").slug("ao-kieu").build());

                // 3. Tạo Sản Phẩm: Áo kiểu voan tay dài kèm hoa
                Product product = Product.builder()
                                .name("Áo kiểu voan tay dài kèm hoa")
                                .category(aoKieu)
                                .basePrice(new BigDecimal("555000"))
                                .description("Áo kiểu voan tay dài kèm hoa mang đến vẻ đẹp nhẹ nhàng, nữ tính...")
                                // Tôi dùng ảnh mẫu placeholder, sau này bạn sẽ thay bằng link Cloudinary thật
                                .thumbnail("https://cdn.hstatic.net/products/1000197303/pro_trang___1__6eda201ee5f948b3af240cc3187bdce5_master.jpg")
                                .build();

                // --- Variant 1: Màu Trắng ---
                ProductVariant whiteVar = ProductVariant.builder()
                                .product(product)
                                .colorName("Trắng")
                                .colorCode("#FFFFFF")
                                .build();

                // Ảnh cho màu trắng
                ProductVariantImage imgWhite1 = ProductVariantImage.builder().variant(whiteVar).imageUrl(
                                "https://cdn.hstatic.net/products/1000197303/pro_trang___1__6eda201ee5f948b3af240cc3187bdce5_master.jpg")
                                .build();
                ProductVariantImage imgWhite2 = ProductVariantImage.builder().variant(whiteVar).imageUrl(
                                "https://cdn.hstatic.net/products/1000197303/pro_trang___3__9f09b60c9fe94ce4bac164437049558f_master.jpg")
                                .build();
                whiteVar.setImages(Arrays.asList(imgWhite1, imgWhite2));

                // Kho hàng cho màu trắng (only create records with quantity > 0)
                ProductStock stockWhiteS = ProductStock.builder().variant(whiteVar).size(s).quantity(23)
                                .sku("AK-TRANG-S").build();
                ProductStock stockWhiteM = ProductStock.builder().variant(whiteVar).size(m).quantity(31)
                                .sku("AK-TRANG-M").build();
                ProductStock stockWhiteL = ProductStock.builder().variant(whiteVar).size(l).quantity(17)
                                .sku("AK-TRANG-L").build();
                // XL is out of stock - no record created
                whiteVar.setStocks(Arrays.asList(stockWhiteS, stockWhiteM, stockWhiteL));

                // --- Variant 2: Màu Đen ---
                ProductVariant blackVar = ProductVariant.builder()
                                .product(product)
                                .colorName("Đen")
                                .colorCode("#000000")
                                .build();

                // Ảnh cho màu đen
                ProductVariantImage imgBlack1 = ProductVariantImage.builder().variant(blackVar).imageUrl(
                                "https://cdn.kkfashion.vn/6035-large_default/ao-voan-den-tay-dai-asm05-08.jpg").build();
                blackVar.setImages(Arrays.asList(imgBlack1));

                // Kho hàng cho màu đen (only create records with quantity > 0)
                ProductStock stockBlackM = ProductStock.builder().variant(blackVar).size(m).quantity(10).sku("AK-DEN-M")
                                .build();
                // S is out of stock - no record created
                blackVar.setStocks(Arrays.asList(stockBlackM));

                // Lưu Product (Cascade sẽ tự lưu Variants, Images và Stock)
                product.setVariants(Arrays.asList(whiteVar, blackVar));
                productRepository.save(product);

                System.out.println("✅ Đã khởi tạo dữ liệu áo kiểu thành công!");
        }
}
