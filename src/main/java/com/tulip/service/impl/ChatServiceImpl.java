package com.tulip.service.impl;

import com.tulip.dto.response.ChatMessageResponseDTO;
import com.tulip.entity.User;
import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;
import com.tulip.entity.product.Product;
import com.tulip.repository.ChatMessageRepository;
import com.tulip.repository.ChatSessionRepository;
import com.tulip.repository.ProductRepository;
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
        String context = getCustomerContext(sessionId);
        String aiRes = generateAIResponse(content, context);
        List<Long> products = recommendProducts(content, context);
        String advice = getPolicyAdvice(content, context);

        updateCustomerContext(sessionId, context + " User: " + content + " | AI: " + aiRes);
        return sendBotResponse(sessionId, aiRes, products, advice);
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

    @Override public void endSession(Long id) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            s.setStatus(ChatSession.SessionStatus.ENDED);
            s.setEndedAt(LocalDateTime.now());
            chatSessionRepository.save(s);
        });
    }

    @Override public List<ChatSession> getUserSessions(User user) {
        return chatSessionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override public void markMessagesAsRead(Long id) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            List<ChatMessage> unread = chatMessageRepository.findBySessionAndSeenOrderByCreatedAtAsc(s, false);
            unread.forEach(m -> m.setSeen(true));
            chatMessageRepository.saveAll(unread);
        });
    }

    @Override public String generateAIResponse(String msg, String ctx) {
        try {
            return googleAIService.generateResponse(msg, ctx).get();
        } catch (Exception e) { return "Tôi sẽ phản hồi bạn ngay!"; }
    }

    @Override public List<Long> recommendProducts(String msg, String ctx) { return new ArrayList<>(); }
    @Override public String getPolicyAdvice(String msg, String ctx) { return null; }
    @Override public void updateCustomerContext(Long id, String ctx) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            s.setCustomerContext(ctx);
            chatSessionRepository.save(s);
        });
    }
    @Override public String getCustomerContext(Long id) {
        return chatSessionRepository.findById(id).map(ChatSession::getCustomerContext).orElse("");
    }
}