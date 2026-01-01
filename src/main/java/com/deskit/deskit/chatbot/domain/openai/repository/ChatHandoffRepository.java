package com.deskit.deskit.chatbot.domain.openai.repository;

import com.deskit.deskit.chatbot.domain.openai.entity.ChatHandoff;
import com.deskit.deskit.chatbot.domain.openai.entity.HandoffStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHandoffRepository extends JpaRepository<ChatHandoff, Long> {

    boolean existsByChatIdAndStatus(Long chatId, HandoffStatus status);
}
