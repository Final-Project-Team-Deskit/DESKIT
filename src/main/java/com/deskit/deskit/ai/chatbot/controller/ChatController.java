package com.deskit.deskit.ai.chatbot.controller;

import com.deskit.deskit.ai.chatbot.openai.entity.ChatMessage;
import com.deskit.deskit.ai.chatbot.openai.entity.ChatInfo;
import com.deskit.deskit.ai.chatbot.openai.service.ChatService;
import com.deskit.deskit.ai.chatbot.openai.service.ConversationService;
import com.deskit.deskit.ai.chatbot.openai.service.OpenAIService;
import com.deskit.deskit.ai.chatbot.rag.dto.ChatRequest;
import com.deskit.deskit.ai.chatbot.rag.dto.ChatResponse;
import com.deskit.deskit.ai.chatbot.rag.entity.RouteDecision;
import com.deskit.deskit.ai.chatbot.rag.service.ChatRoutingService;
import com.deskit.deskit.ai.chatbot.rag.service.RagIngestService;
import com.deskit.deskit.ai.chatbot.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RagIngestService ragIngestService;
    private final OpenAIService openAIService;
    private final ChatService chatService;
    private final RagService ragService;
    private final ChatRoutingService chatRoutingService;
    private final ConversationService conversationService;

    // 채팅 페이지 접속
//    @GetMapping("/chat")
//    public String chatPage() {
//        return "chat";
//    }

    // 논 스트림
    @ResponseBody
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String question = request.getQuestion();
        String loginId = "dyniiyeyo@naver.com";
        Long memberId = 1L;

        // 현재 진행 중인 대화 조회 or 생성
        ChatInfo chatInfo = conversationService.getOrCreateActiveConversation(memberId);
        if (chatInfo.getStatus() != com.deskit.deskit.ai.chatbot.openai.entity.ConversationStatus.BOT_ACTIVE) {
            return ChatResponse.builder()
                    .answer("상담 진행 중입니다. 관리자 상담은 종료 후 자동으로 시작됩니다.")
                    .escalated(true)
                    .build();
        }

        if (question == null || question.isBlank()) {
            log.warn("Empty question received: {}", request);
            return null;
        }

        RouteDecision decision = chatRoutingService.decide(question);
        log.info("route={} score={} reason={} q={}",
                decision.route(), decision.score(), decision.reason(), question);


        switch (decision.route()) {
            // RAG로 판단
            case RAG -> {
                return ragService.chat(question, 4);
            }
            // LLM 채팅으로 판단
            case GENERAL -> {
                return openAIService.generate(request.getQuestion());
            }
        }
        return null;
    }

    // 스트림
//    @ResponseBody
//    @PostMapping("/chat/stream")
//    public Flux<String> streamChat(@RequestBody Map<String, String> body) {
//        return openAIService.generateStream(body.get("text"));
//    }

    @ResponseBody
    @PostMapping("/chat/history/{chatId}")
    public List<ChatMessage> getChatHistory(@PathVariable("chatId") Long chatId) {
        return chatService.readAllChats(chatId);
    }

    @PostMapping("/admin/rag/upload")
    public ResponseEntity<?> uploadRagDocument(
            @RequestParam("file") MultipartFile file
//            @RequestParam("category") String category // POLICY, FAQ, NOTICE
    ) {
        ragIngestService.ingest(file);
        return ResponseEntity.ok("업로드 완료");
    }

    @ResponseBody
    @GetMapping("/chat/status/{memberId}")
    public Map<String, String> getStatus(@PathVariable Long memberId) {

        return conversationService.findLatestConversation(memberId)
                .map(c -> Map.of("status", c.getStatus().name()))
                .orElse(Map.of("status", "BOT_ACTIVE"));
    }


}
