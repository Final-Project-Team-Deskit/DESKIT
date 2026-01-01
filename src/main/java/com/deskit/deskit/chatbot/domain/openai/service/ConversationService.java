package com.deskit.deskit.chatbot.domain.openai.service;

import com.deskit.deskit.chatbot.domain.openai.entity.ChatInfo;
import com.deskit.deskit.chatbot.domain.openai.entity.ConversationStatus;
import com.deskit.deskit.chatbot.domain.rag.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Transactional
    public ChatInfo getOrCreateActiveConversation(Long memberId) {

        return conversationRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .filter(c -> c.getStatus() == ConversationStatus.BOT_ACTIVE)
                .orElseGet(() -> {
                    ChatInfo c = new ChatInfo();
                    c.setMemberId(memberId);
                    c.setStatus(ConversationStatus.BOT_ACTIVE);
                    return conversationRepository.save(c);
                });
    }

    @Transactional(readOnly = true)
    public ChatInfo getLatestConversation(Long memberId) {
        return conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new IllegalStateException("Conversation not found for memberId=" + memberId));
    }

    @Transactional(readOnly = true)
    public Optional<ChatInfo> findLatestConversation(Long memberId) {
        return conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId);
    }

}

