package com.deskit.deskit.ai.chatbot.rag.service;

import com.deskit.deskit.ai.config.RagVectorProperties;
import com.deskit.deskit.ai.chatbot.openai.entity.ChatInfo;
import com.deskit.deskit.ai.chatbot.openai.service.ConversationService;
import com.deskit.deskit.ai.chatbot.rag.dto.ChatResponse;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class RagService {

    private static final String MODEL = "gpt-4o-mini";
    private static final double TEMPERATURE = 0.7;
    private static final String NO_CONTEXT_MESSAGE =
            "해당 내용은 현재 제공된 정보로는 안내드릴 수 없어요. \"관리자 연결\"을 입력하시면 관리자에게 문의가 접수돼요.";

    private final RedisVectorStore vectorStore;
    private final RagVectorProperties properties;
    private final AdminEscalationService adminEscalationService;
    private final ConversationService conversationService;
    private final ChatSaveService chatSaveService;
    private final ChatMemoryRepository chatMemoryRepository;
    private final OpenAIClient openAIClient;

    private static final String ESCALATION_TRIGGER = "관리자 연결";

    public ChatResponse chat(String question, int topK) {
        Long memberId = 1L;

        ChatInfo chatInfo = conversationService.getOrCreateActiveConversation(memberId);

        boolean escalated = false;

        String normalized = normalize(question);
        if (normalized != null && normalized.equals(ESCALATION_TRIGGER)) {
            log.info("Escalation triggered");
            return adminEscalationService.escalate(normalized, chatInfo.getChatId(), String.valueOf(memberId));
        }

        int candidates = topK > 0 ? topK : properties.getTopK();
        log.info("[RAG] chat() called. question={}", normalized);

        if (normalized == null || normalized.isBlank()) {
            return new ChatResponse(NO_CONTEXT_MESSAGE, List.of(), false);
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(normalized)
                .topK(candidates)
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        log.info("[RAG] similaritySearch result size={}", documents.size());

        if (documents.isEmpty()) {
            return new ChatResponse(NO_CONTEXT_MESSAGE, List.of(), false);
        }

        String context = buildContext(documents);
        if (context.isBlank()) {
            return new ChatResponse(NO_CONTEXT_MESSAGE, List.of(), false);
        }

        List<Message> messages = buildMessages(context, normalized);

        String answer;
        try {
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(MODEL)
                    .inputOfResponse(buildResponseInput(messages))
                    .temperature(TEMPERATURE)
                    .build();
            Response response = openAIClient.responses().create(params);
            answer = extractOutputText(response);
        } catch (OpenAIException e) {
            log.error("RAG OpenAI error", e);
            return new ChatResponse(
                    "현재 상담 시스템에 문제가 발생했어요. 잠시 후 다시 시도해 주세요.",
                    List.of(),
                    false
            );
        }
        if (answer == null || answer.isBlank()) {
            answer = NO_CONTEXT_MESSAGE;
        }
        chatSaveService.saveChat(chatInfo.getChatId(), question, answer);
        chatSaveService.saveChatMemory(String.valueOf(memberId), question, chatMemoryRepository);

        List<String> sources = documents.stream()
                .map(doc -> String.valueOf(doc.getMetadata().getOrDefault("source", "")))
                .toList();

        return new ChatResponse(answer, sources, escalated);
    }

    private String buildContext(List<Document> documents) {
        return documents.stream()
                .limit(3)
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"))
                .trim();
    }

    private List<Message> buildMessages(String context, String question) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(
                """
                        당신은 고객 지원을 돕는 AI 상담 챗봇입니다.
                        
                        문서에 질문과 정확히 일치하는 내용이 없더라도,
                        관련된 정보가 문서에 일부라도 포함되어 있다면
                        그 범위 내에서만 요약하여 안내하세요.
                        
                        단, 문서에 전혀 근거가 없는 내용은
                        추측하거나 일반적인 답변을 하지 마세요.
                        
                        다음과 같은 경우에는 직접 답변하지 말고,
                        관리자 상담이 필요하다는 안내를 하세요.
                        
                        1. 제공된 문서(Context)에 근거가 없는 질문인 경우
                        2. 사용자의 개인적인 상황, 주문 내역, 결제 정보, 계정 상태 등
                           개인 정보 또는 개인별 처리가 필요한 질문인 경우
                        3. 정책 문서에 없는 예외 처리, 임의 판단, 특수 요청을 요구하는 경우
                        4. 문서 내용만으로 정확하고 책임 있는 답변을 제공할 수 없다고 판단되는 경우
                        5. "관리자 문의", "사람이랑 상담", "직접 문의하고 싶다" 등 고객이 명시적으로 관리자 상담을 요청하는 경우
                        
                        위 조건에 해당하는 경우에는
                        반드시 아래 문장 중 하나의 형태로만 응답하세요.
                        
                        - 해당 내용은 현재 제공된 정보로는 안내드릴 수 없어요. "관리자 연결"을 입력하시면 관리자에게 문의가 접수돼요.
                        - 개인 정보 또는 개별 확인이 필요한 내용으로, 관리자 상담을 통해 안내가 가능해요. "관리자 연결"을 입력하시면 관리자에게 문의가 접수돼요.
                        - 요청하신 내용은 관리자 확인이 필요하여 상담 연결이 필요해요. "관리자 연결"을 입력하시면 관리자에게 문의가 접수돼요.
                        - 관리자와 직접 상담을 원하시는 것 같아요. "관리자 연결"을 입력하시면 관리자에게 문의가 접수돼요.
                        
                        절대 위 문구 외의 임의의 답변을 생성하지 마세요.
                        
                        """
        ));
        messages.add(new UserMessage("""
                [Context]
                %s
                
                [Question]
                %s
                """.formatted(context, question)));
        return messages;
    }

    private String normalize(String question) {
        return question == null ? null : question.trim();
    }

    private List<ResponseInputItem> buildResponseInput(List<Message> messages) {
        List<ResponseInputItem> inputItems = new ArrayList<>();
        for (Message message : messages) {
            EasyInputMessage easyMessage = EasyInputMessage.builder()
                    .role(toEasyInputRole(message))
                    .content(getMessageText(message))
                    .build();
            inputItems.add(ResponseInputItem.ofEasyInputMessage(easyMessage));
        }
        return inputItems;
    }

    private EasyInputMessage.Role toEasyInputRole(Message message) {
        return switch (message.getMessageType()) {
            case SYSTEM -> EasyInputMessage.Role.SYSTEM;
            case USER -> EasyInputMessage.Role.USER;
            case ASSISTANT -> EasyInputMessage.Role.ASSISTANT;
            case TOOL -> EasyInputMessage.Role.SYSTEM;
        };
    }

    private String getMessageText(Message message) {
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.getText();
        }
        if (message instanceof UserMessage userMessage) {
            return userMessage.getText();
        }
        if (message instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        return "";
    }

    private String extractOutputText(Response response) {
        if (response == null || response.output() == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (ResponseOutputItem item : response.output()) {
            if (!item.isMessage()) {
                continue;
            }
            for (ResponseOutputMessage.Content content : item.asMessage().content()) {
                if (content.isOutputText()) {
                    text.append(content.asOutputText().text());
                }
            }
        }
        return text.toString();
    }

    public void ingest(List<Document> documents) {
        vectorStore.add(documents);
        log.info("vectorStore added: " + documents);
    }

    public Document createDocument(String content, Map<String, Object> metadata) {
        return new Document(content, metadata);
    }
}
