package com.tulip.service.impl;

import com.tulip.dto.response.ChatMessageResponseDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;
import com.tulip.entity.product.Product;
import com.tulip.repository.ChatMessageRepository;
import com.tulip.repository.ChatSessionRepository;
import com.tulip.repository.ProductRepository;
import com.tulip.service.ChatContextBuilderService;
import com.tulip.service.ChatService;
import com.tulip.service.GoogleAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final GoogleAIService googleAIService;
    private final ChatContextBuilderService contextBuilderService;

    @Override
    public ChatSession createSession(User user, String customerName, String customerEmail) {
        ChatSession session = ChatSession.builder()
                .user(user)
                .sessionToken(UUID.randomUUID().toString())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .status(ChatSession.SessionStatus.ACTIVE)
                .customerContext("")
                .build();
        return chatSessionRepository.save(session);
    }

    @Override
    public Optional<ChatSession> getSessionByToken(String sessionToken) {
        return chatSessionRepository.findBySessionToken(sessionToken);
    }

    @Override
    public ChatSession getSessionOrCreate(User user, String sessionToken) {
        if (sessionToken != null) {
            return getSessionByToken(sessionToken)
                    .filter(s -> s.getStatus() == ChatSession.SessionStatus.ACTIVE)
                    .orElseGet(() -> createSession(user, null, null));
        }
        return createSession(user, null, null);
    }

    @Override
    public ChatMessageResponseDTO sendMessage(Long sessionId, String content, ChatMessage.MessageType messageType) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .messageType(messageType)
                .senderType(ChatMessage.SenderType.CUSTOMER)
                .content(content)
                .sender(session.getUser())
                .seen(false)
                .build();

        return convertToDTO(chatMessageRepository.save(message));
    }

    @Override
    public ChatMessageResponseDTO sendCustomerMessage(Long sessionId, String content) {
        sendMessage(sessionId, content, ChatMessage.MessageType.TEXT);
        
        // === BẮT ĐẦU LOGIC MỚI: LẤY DỮ LIỆU THỰC TỪ DATABASE ===
        log.info("📩 Nhận tin nhắn: {}", content);
        
        // 1. Lấy context cũ (lịch sử chat)
        String existingContext = getCustomerContext(sessionId);
        
        // 2. BUILD FULL CONTEXT TỪ DATABASE (RAG)
        // Đây là bước QUAN TRỌNG: Context builder sẽ query DB và lấy dữ liệu thực
        String fullContext = contextBuilderService.buildFullContext(content, existingContext);
        log.info("✅ Đã build context với {} ký tự từ database", fullContext.length());
        
        // 3. Tìm sản phẩm gợi ý (logic cũ giữ lại)
        List<Long> productIds = recommendProducts(content, existingContext);
        
        // 4. Lấy policy advice nếu có
        String policyAdvice = getPolicyAdvice(content, existingContext);
        
        // 5. Generate AI response với FULL CONTEXT từ database
        String aiRes;
        String lower = content == null ? "" : content.toLowerCase();
        
        if (policyAdvice != null && !policyAdvice.isBlank() && containsBodyMeasurement(lower) && isProductRequest(lower)) {
            // Trường hợp: hỏi size + body measurement + muốn sản phẩm
            aiRes = policyAdvice + "\n\nMình gợi ý một vài mẫu phù hợp bên dưới, bạn xem giúp mình nhé.";
        } else if (policyAdvice != null && !policyAdvice.isBlank() && isPolicyOrSizeQuestion(lower) && !isProductRequest(lower)) {
            // Trường hợp: chỉ hỏi policy/size, không cần sản phẩm
            aiRes = policyAdvice;
        } else {
            // Trường hợp: câu hỏi thông thường => Gọi AI với FULL CONTEXT
            aiRes = generateAIResponse(content, fullContext);
            log.info("🤖 AI đã trả lời dựa trên {} sản phẩm từ database", 
                fullContext.contains("DANH SÁCH SẢN PHẨM") ? "nhiều" : "0");
        }
        
        // 6. Cập nhật context (lưu lịch sử)
        updateCustomerContext(sessionId, existingContext + " User: " + content + " | AI: " + aiRes);
        
        return sendBotResponse(sessionId, aiRes, productIds, policyAdvice);
    }

    @Override
    public ChatMessageResponseDTO sendBotResponse(Long sessionId, String content, List<Long> recommendedProductIds, String policyAdvice) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        ChatMessage botMessage = ChatMessage.builder()
                .session(session)
                .messageType(ChatMessage.MessageType.TEXT)
                .senderType(ChatMessage.SenderType.AI_BOT)
                .content(content)
                .aiResponse(content)
                .policyAdvice(policyAdvice)
                .seen(false)
                .build();

        if (recommendedProductIds != null && !recommendedProductIds.isEmpty()) {
            botMessage.setRecommendedProducts(productRepository.findAllById(recommendedProductIds));
            botMessage.setMessageType(ChatMessage.MessageType.PRODUCT_RECOMMENDATION);
        }

        return convertToDTO(chatMessageRepository.save(botMessage));
    }

    @Override
    public List<ChatMessageResponseDTO> getSessionMessages(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)
                .stream().map(this::convertToDTO).toList();
    }

    private ChatMessageResponseDTO convertToDTO(ChatMessage message) {
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderType(message.getSenderType().name())
                .messageType(message.getMessageType().name())
                .aiResponse(message.getAiResponse())
                .policyAdvice(message.getPolicyAdvice())
                .createdAt(message.getCreatedAt())
                .seen(message.getSeen())
                .recommendedProductIds(message.getRecommendedProducts().stream().map(Product::getId).toList())
                .build();
    }

    @Override
    public void endSession(Long id) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            s.setStatus(ChatSession.SessionStatus.ENDED);
            s.setEndedAt(LocalDateTime.now());
            chatSessionRepository.save(s);
        });
    }

    @Override
    public List<ChatSession> getUserSessions(User user) {
        return chatSessionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public void markMessagesAsRead(Long id) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            List<ChatMessage> unread = chatMessageRepository.findBySessionAndSeenOrderByCreatedAtAsc(s, false);
            unread.forEach(m -> m.setSeen(true));
            chatMessageRepository.saveAll(unread);
        });
    }

    @Override
    public String generateAIResponse(String msg, String ctx) {
        try {
            return googleAIService.generateResponse(msg, ctx).get();
        } catch (Exception e) {
            return "Tôi sẽ phản hồi bạn ngay!";
        }
    }

    @Override
    public List<Long> recommendProducts(String msg, String ctx) {
        if (msg == null) return new ArrayList<>();
        String lower = msg.toLowerCase();
        List<String> keywords = new ArrayList<>();
        if (lower.contains("đầm") || lower.contains("váy")) {
            keywords.add("váy");
            keywords.add("đầm");
        }
        if (lower.contains("áo") || lower.contains("blouse") || lower.contains("shirt")) {
            keywords.add("áo");
        }
        if (lower.contains("quần") || lower.contains("pants")) {
            keywords.add("quần");
        }
        if (lower.contains("set") || lower.contains("bộ")) {
            keywords.add("set");
        }

        List<Product> found = new ArrayList<>();
        for (String kw : keywords) {
            found.addAll(productRepository.searchSmart(kw));
        }

        if (found.isEmpty() && isProductRequest(lower)) {
            found.addAll(productRepository.findTop5ByOrderByIdDesc());
        }

        return found.stream().distinct().limit(3).map(Product::getId).toList();
    }

    @Override
    public String getPolicyAdvice(String msg, String ctx) {
        if (msg == null) return null;
        String lower = msg.toLowerCase();
        if (lower.contains("size") || lower.contains("kích thước") || lower.contains("size guide") || lower.contains("bảng size")
                || containsBodyMeasurement(lower)) {
            return """
                💡 Bảng size tham khảo (chung, có thể khác biệt nhẹ theo từng mẫu):
                - S: Vòng ngực 86, vai 38, dài 65
                - M: Vòng ngực 90, vai 40, dài 67
                - L: Vòng ngực 96, vai 42, dài 69
                - XL: Vòng ngực 102, vai 44, dài 71
                Bạn cho mình xin thêm chiều cao + số đo vòng ngực/vòng eo (hoặc mẫu bạn muốn mua) để mình chốt size chính xác hơn nhé.
                """;
        }

        if (lower.contains("bảo hành") || lower.contains("warranty")) {
            return """
                🛡️ Chính sách bảo hành:
                - Bảo hành 30 ngày đối với lỗi từ nhà sản xuất (bung chỉ, lỗi đường may, lỗi chất liệu).
                - Không bảo hành cho lỗi do sử dụng (rách, ố, phai màu do giặt không đúng cách).
                """;
        }
        if (lower.contains("đổi trả") || lower.contains("đổi hàng") || lower.contains("đổi size")
                || lower.contains("trả hàng") || lower.contains("trả lại")
                || lower.contains("return") || lower.contains("refund") || lower.contains("hoàn tiền")) {
            return """
                🔄 Đổi trả & Hoàn tiền:
                - Đổi size miễn phí trong 7 ngày (sản phẩm chưa qua sử dụng, còn tag).
                - Trả lại hàng trong vòng 14 ngày nếu có lỗi từ nhà sản xuất.
                - Hoàn tiền trong 5-7 ngày làm việc sau khi nhận lại sản phẩm.
                """;
        }
        if (lower.contains("thanh toán") || lower.contains("payment") || lower.contains("cod")) {
            return """
                💳 Phương thức thanh toán:
                - COD (thanh toán khi nhận hàng)
                - Chuyển khoản ngân hàng
                - Ví điện tử (Momo, ZaloPay, VNPay)
                - Thẻ tín dụng/ghi nợ
                """;
        }
        if (lower.contains("vận chuyển") || lower.contains("giao hàng") || lower.contains("shipping")) {
            return """
                🚚 Vận chuyển:
                - Nội thành Hà Nội: 1-2 ngày, phí 25k (đơn từ 500k miễn phí).
                - Các tỉnh thành khác: 3-5 ngày, phí 35k (đơn từ 700k miễn phí).
                - Giao hàng hỏa tốc (nếu có): 24h, phí 80k.
                """;
        }
        return null;
    }

    private boolean isPolicyOrSizeQuestion(String lower) {
        return lower.contains("size")
                || lower.contains("kích thước")
                || lower.contains("bảng size")
                || lower.contains("size guide")
                || containsBodyMeasurement(lower)

                || lower.contains("đổi trả")
                || lower.contains("đổi hàng")
                || lower.contains("đổi size")
                || lower.contains("trả hàng")
                || lower.contains("trả lại")
                || lower.contains("return")
                || lower.contains("refund")
                || lower.contains("hoàn tiền")
                || lower.contains("bảo hành")
                || lower.contains("warranty")
                || lower.contains("thanh toán")
                || lower.contains("payment")
                || lower.contains("cod")
                || lower.contains("vận chuyển")
                || lower.contains("giao hàng")
                || lower.contains("shipping");
    }

    private boolean isProductRequest(String lower) {
        return lower.contains("sản phẩm")
                || lower.contains("mẫu")
                || lower.contains("gợi ý")
                || lower.contains("recommend")
                || lower.contains("phù hợp")
                || lower.contains("tư vấn");
    }

    private boolean containsBodyMeasurement(String lower) {
        return lower.matches(".*\\b\\d{2,3}\\s?kg\\b.*")
                || lower.matches(".*\\b\\d{2,3}\\s?cm\\b.*")
                || lower.matches(".*(nặng|cân nặng)\\s*\\d{2,3}.*")
                || lower.matches(".*(cao|chiều cao)\\s*\\d{2,3}.*");
    }

    @Override
    public void updateCustomerContext(Long id, String ctx) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            s.setCustomerContext(ctx);
            chatSessionRepository.save(s);
        });
    }
    @Override public String getCustomerContext(Long id) {
        return chatSessionRepository.findById(id).map(ChatSession::getCustomerContext).orElse("");
    }
}