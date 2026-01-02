package com.tulip.service;

import com.tulip.dto.response.ChatMessageResponseDTO;
import com.tulip.entity.chat.ChatMessage;
import com.tulip.entity.chat.ChatSession;
import com.tulip.entity.User;
import java.util.List;
import java.util.Optional;

public interface ChatService {
    ChatSession createSession(User user, String customerName, String customerEmail);
    Optional<ChatSession> getSessionByToken(String sessionToken);
    ChatSession getSessionOrCreate(User user, String sessionToken);
    void endSession(Long sessionId);
    List<ChatSession> getUserSessions(User user);

    ChatMessageResponseDTO sendMessage(Long sessionId, String content, ChatMessage.MessageType messageType);
    ChatMessageResponseDTO sendCustomerMessage(Long sessionId, String content);
    ChatMessageResponseDTO sendBotResponse(Long sessionId, String content, List<Long> recommendedProductIds, String policyAdvice);
    List<ChatMessageResponseDTO> getSessionMessages(Long sessionId);
    void markMessagesAsRead(Long sessionId);

    String generateAIResponse(String customerMessage, String customerContext);
    List<Long> recommendProducts(String customerMessage, String customerContext);
    String getPolicyAdvice(String customerMessage, String customerContext);
    void updateCustomerContext(Long sessionId, String context);
    String getCustomerContext(Long sessionId);
}