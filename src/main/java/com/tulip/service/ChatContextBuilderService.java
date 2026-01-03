package com.tulip.service;

import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductStatus;
import com.tulip.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chuyên trách xây dựng Context từ Database cho AI
 * Đây là "bộ não" thu thập thông tin thực tế để AI trả lời chính xác
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatContextBuilderService {

    private final ProductRepository productRepository;

    /**
     * Build full context cho AI dựa trên câu hỏi của khách hàng
     * @param userMessage Tin nhắn của khách
     * @param existingContext Context cũ (lịch sử chat)
     * @return Context đầy đủ để AI trả lời
     */
    public String buildFullContext(String userMessage, String existingContext) {
        StringBuilder context = new StringBuilder();
        
        // 1. Thông tin cơ bản về shop (STATIC)
        context.append(getShopBasicInfo()).append("\n\n");
        
        // 2. Chính sách shop (STATIC)
        context.append(getShopPolicies()).append("\n\n");
        
        // 3. Dữ liệu sản phẩm THỰC TẾ từ Database (DYNAMIC)
        String productContext = buildProductContext(userMessage);
        if (!productContext.isEmpty()) {
            context.append(productContext).append("\n\n");
        }
        
        // 4. Context cũ (lịch sử hội thoại)
        if (existingContext != null && !existingContext.isEmpty()) {
            context.append("LỊCH SỬ HỘI THOẠI:\n").append(existingContext);
        }
        
        return context.toString();
    }

    /**
     * Thông tin cơ bản về shop (có thể lưu trong database hoặc config)
     */
    private String getShopBasicInfo() {
        return """
            📍 THÔNG TIN TULIP SHOP:
            - Tên: Tulip Shop - Thời trang nữ
            - Chuyên: Áo, váy, quần, đầm, set đồ công sở & đi chơi
            - Phong cách: Trẻ trung, thanh lịch, hiện đại
            - Hotline: 0123.456.789
            - Email: contact@tulipshop.vn
            """;
    }

    /**
     * Chính sách shop
     */
    private String getShopPolicies() {
        return """
            📋 CHÍNH SÁCH SHOP:
            
            🔄 Đổi trả:
            - Đổi size miễn phí trong 7 ngày (sản phẩm chưa qua sử dụng, còn tag)
            - Trả hàng trong 14 ngày nếu có lỗi từ nhà sản xuất
            - Hoàn tiền 5-7 ngày làm việc
            
            🛡️ Bảo hành:
            - Bảo hành 30 ngày lỗi nhà sản xuất (bung chỉ, lỗi đường may)
            - Không bảo hành lỗi do sử dụng
            
            🚚 Vận chuyển:
            - Nội thành HN: 1-2 ngày, phí 25k (đơn từ 500k miễn phí)
            - Tỉnh khác: 3-5 ngày, phí 35k (đơn từ 700k miễn phí)
            - Giao hỏa tốc: 24h, phí 80k
            
            💳 Thanh toán:
            - COD, Chuyển khoản, Momo, ZaloPay, VNPay, Thẻ
            
            📏 Size guide:
            - S: Vòng ngực 86cm, vai 38cm, dài 65cm (phù hợp 45-50kg, cao 1m50-1m58)
            - M: Vòng ngực 90cm, vai 40cm, dài 67cm (phù hợp 51-55kg, cao 1m58-1m65)
            - L: Vòng ngực 96cm, vai 42cm, dài 69cm (phù hợp 56-62kg, cao 1m65-1m70)
            - XL: Vòng ngực 102cm, vai 44cm, dài 71cm (phù hợp 63-70kg, cao 1m70+)
            """;
    }

    /**
     * Build product context từ Database dựa trên câu hỏi
     * ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT - TRUY VẤN DATABASE THỰC TẾ
     */
    private String buildProductContext(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "";
        }

        String lower = userMessage.toLowerCase();
        List<Product> relevantProducts = new ArrayList<>();

        // === PHÂN TÍCH CÂU HỎI VÀ TÌM SẢN PHẨM LIÊN QUAN ===
        
        // 1. Nếu hỏi về áo
        if (lower.matches(".*(áo|blouse|shirt|sơ mi|thun|kiểu).*")) {
            log.info("🔍 Tìm áo theo keywords");
            relevantProducts.addAll(productRepository.searchSmart("áo"));
        }
        
        // 2. Nếu hỏi về váy/đầm
        if (lower.matches(".*(váy|đầm|dress|maxi|midi).*")) {
            log.info("🔍 Tìm váy/đầm theo keywords");
            relevantProducts.addAll(productRepository.searchSmart("váy"));
            relevantProducts.addAll(productRepository.searchSmart("đầm"));
        }
        
        // 3. Nếu hỏi về quần
        if (lower.matches(".*(quần|pants|jeans|short).*")) {
            log.info("🔍 Tìm quần theo keywords");
            relevantProducts.addAll(productRepository.searchSmart("quần"));
        }
        
        // 4. Nếu hỏi về set/bộ
        if (lower.matches(".*(set|bộ|combo).*")) {
            log.info("🔍 Tìm set/bộ theo keywords");
            relevantProducts.addAll(productRepository.searchSmart("set"));
        }
        
        // 5. Nếu hỏi về đồ công sở
        if (lower.matches(".*(công sở|office|formal|đi làm).*")) {
            log.info("🔍 Tìm đồ công sở theo tags");
            relevantProducts.addAll(productRepository.findByTagsContainingIgnoreCase("công sở"));
            relevantProducts.addAll(productRepository.findByTagsContainingIgnoreCase("di-lam"));
        }
        
        // 6. Nếu hỏi về đồ đi chơi/dạo phố
        if (lower.matches(".*(đi chơi|dạo phố|street|casual|đi học).*")) {
            log.info("🔍 Tìm đồ dạo phố theo tags");
            relevantProducts.addAll(productRepository.findByTagsContainingIgnoreCase("dạo phố"));
            relevantProducts.addAll(productRepository.findByTagsContainingIgnoreCase("đi chơi"));
        }
        
        // 7. Nếu hỏi giá rẻ/sale/khuyến mãi
        if (lower.matches(".*(giá rẻ|sale|khuyến mãi|giảm giá|ưu đãi|deal).*")) {
            log.info("🔍 Tìm sản phẩm sale");
            relevantProducts.addAll(productRepository.findProductsDiscountOver18(ProductStatus.ACTIVE));
        }
        
        // 8. Nếu hỏi hàng mới/mới về
        if (lower.matches(".*(mới về|hàng mới|new arrival|mẫu mới).*")) {
            log.info("🔍 Tìm hàng mới");
            relevantProducts.addAll(productRepository.findTop5ByStatusOrderByIdDesc(ProductStatus.ACTIVE));
        }
        
        // 9. Nếu hỏi bán chạy/hot/thịnh hành
        if (lower.matches(".*(bán chạy|hot|thịnh hành|trending|phổ biến|yêu thích).*")) {
            log.info("🔍 Tìm sản phẩm bán chạy");
            relevantProducts.addAll(productRepository.findBestSellingProducts());
        }
        
        // 10. Nếu câu hỏi chung hoặc không match gì => lấy random
        if (relevantProducts.isEmpty() && isGeneralProductQuestion(lower)) {
            log.info("🔍 Câu hỏi chung, lấy sản phẩm ngẫu nhiên");
            relevantProducts.addAll(productRepository.findRandomActiveProducts());
        }

        // Remove duplicates và giới hạn số lượng
        List<Product> uniqueProducts = relevantProducts.stream()
                .distinct()
                .limit(8) // Giới hạn 8 sản phẩm để context không quá dài
                .toList();

        if (uniqueProducts.isEmpty()) {
            return "";
        }

        // === FORMAT SẢN PHẨM THÀNH TEXT CHO AI ===
        return formatProductsForAI(uniqueProducts);
    }

    /**
     * Format danh sách sản phẩm thành text dễ đọc cho AI
     */
    private String formatProductsForAI(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛍️ DANH SÁCH SẢN PHẨM HIỆN CÓ (").append(products.size()).append(" sản phẩm):\n");
        
        int index = 1;
        for (Product p : products) {
            sb.append(String.format("%d. %s\n", index++, p.getName()));
            
            // Giá
            if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = p.getBasePrice().subtract(p.getDiscountPrice());
                int percent = discount.multiply(BigDecimal.valueOf(100))
                    .divide(p.getBasePrice(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
                sb.append(String.format("   💰 Giá: %,d₫ (Gốc: %,d₫) - GIẢM %d%%\n", 
                    p.getDiscountPrice().longValue(), p.getBasePrice().longValue(), percent));
            } else {
                sb.append(String.format("   💰 Giá: %,d₫\n", p.getBasePrice().longValue()));
            }
            
            // Mô tả ngắn
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                String desc = p.getDescription();
                if (desc.length() > 80) {
                    desc = desc.substring(0, 80) + "...";
                }
                sb.append("   📝 ").append(desc).append("\n");
            }
            
            // Màu sắc có sẵn
            if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                String colors = p.getVariants().stream()
                    .map(v -> v.getColorName())
                    .distinct()
                    .collect(Collectors.joining(", "));
                sb.append("   🎨 Màu: ").append(colors).append("\n");
            }
            
            // Tags
            if (p.getTags() != null && !p.getTags().isEmpty()) {
                sb.append("   🏷️ Tags: ").append(p.getTags()).append("\n");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Kiểm tra xem có phải câu hỏi chung về sản phẩm không
     */
    private boolean isGeneralProductQuestion(String lower) {
        return lower.matches(".*(có gì|bán gì|sản phẩm|mẫu|gợi ý|tư vấn|mua|xem|show).*");
    }

    /**
     * Build context nhanh cho trường hợp đã có product IDs
     */
    public String buildContextForProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return "";
        }
        
        List<Product> products = productRepository.findAllById(productIds);
        return formatProductsForAI(products);
    }
}
