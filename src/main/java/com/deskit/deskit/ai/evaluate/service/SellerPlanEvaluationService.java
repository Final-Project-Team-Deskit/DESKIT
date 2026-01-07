package com.deskit.deskit.ai.evaluate.service;

import com.deskit.deskit.account.entity.SellerRegister;
import com.deskit.deskit.ai.config.RagVectorProperties;
import com.deskit.deskit.ai.evaluate.dto.EvaluateDTO;
import com.deskit.deskit.ai.evaluate.entity.AiEvaluation;
import com.deskit.deskit.ai.evaluate.repository.AiEvalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerPlanEvaluationService {

    private static final String MODEL = "gpt-4o-mini";

    // Vector store for policy RAG context lookup.
    private final RedisVectorStore vectorStore;
    // OpenAI client used to generate the evaluation.
    private final OpenAIClient openAIClient;
    // RAG configuration for retrieval size.
    private final RagVectorProperties ragVectorProperties;
    // Tool collection for structured evaluation output.
    private final AiTools chatTools;
    // Repository used to persist AI evaluation results.
    private final AiEvalRepository aiEvalRepository;
    private final ObjectMapper objectMapper;

    // Evaluate a seller business plan and persist the AI result.
    public AiEvaluation evaluateAndSave(SellerRegister registerEntity) {
        if (registerEntity == null) {
            throw new IllegalArgumentException("seller register is required");
        }
        if (registerEntity.getPlanFile() == null || registerEntity.getPlanFile().length == 0) {
            throw new IllegalArgumentException("business plan file is required");
        }

        // Extract plan text for evaluation context.
        String planText = extractPlanText(registerEntity.getPlanFile());
        // Retrieve policy context using RAG similarity search.
        String policyContext = buildPolicyContext(planText);

        // System prompt guides AI to use policy context and tool output.
        SystemMessage systemMessage = new SystemMessage("""
                당신은 DESKIT 플랫폼 판매자 회원가입 심사를 담당하는 AI입니다.
                제공된 사업계획서와 정책 문서(Context)를 바탕으로만 평가하세요.
                문서 근거가 부족하면 요약에 그 이유를 명시하고, 추측은 하지 마세요.

                점수는 0~20 범위로 작성하고, total_score는 항목 합계로 작성하세요.
                gradeRecommended는 SellerGrade 열거형 값 중 하나로 지정하세요.

                반드시 getEvaluateResultTool 함수를 호출해서 결과를 반환하세요.
                응답은 아래 JSON 형식으로만 출력하세요.
                {
                  "businessStability": 0,
                  "productCompetency": 0,
                  "liveSuitability": 0,
                  "operationCoop": 0,
                  "growthPotential": 0,
                  "totalScore": 0,
                  "gradeRecommended": "A|B|C|REJECTED",
                  "summary": "..."
                }
                """);

        // User prompt contains seller data, plan text, and policy context.
        UserMessage userMessage = new UserMessage("""
                [판매자 정보]
                회사명: %s
                설명: %s

                [사업계획서]
                %s

                [정책 문서 발췌]
                %s
                """.formatted(
                nullSafe(registerEntity.getCompanyName()),
                nullSafe(registerEntity.getDescription()),
                planText,
                policyContext
        ));

        // Execute evaluation with tool support for structured output.
        EvaluateDTO evaluateDTO = requestEvaluation(systemMessage, userMessage);

        // Map DTO to entity for persistence.
        AiEvaluation evaluation = toEntity(evaluateDTO, registerEntity);
        return aiEvalRepository.save(evaluation);
    }

    // Convert evaluation DTO to persisted entity.
    private AiEvaluation toEntity(EvaluateDTO evaluateDTO, SellerRegister registerEntity) {
        AiEvaluation evaluation = new AiEvaluation();
        evaluation.setBusinessStability(evaluateDTO.businessStability());
        evaluation.setProductCompetency(evaluateDTO.productCompetency());
        evaluation.setLiveSuitability(evaluateDTO.liveSuitability());
        evaluation.setOperationCoop(evaluateDTO.operationCoop());
        evaluation.setGrowthPotential(evaluateDTO.growthPotential());
        evaluation.setTotalScore(evaluateDTO.total_score());
        evaluation.setSellerGrade(evaluateDTO.gradeRecommended());
        evaluation.setSummary(evaluateDTO.summary());
        evaluation.setSellerId(registerEntity.getSellerId());
        evaluation.setRegisterId(registerEntity.getRegisterId());
        return evaluation;
    }

    // Extract plain text from the uploaded business plan.
    private String extractPlanText(byte[] planFile) {
        // Convert byte array into a readable resource for Tika.
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(planFile));
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    // Build a policy context string using similarity search.
    private String buildPolicyContext(String planText) {
        int topK = ragVectorProperties.getTopK() > 0 ? ragVectorProperties.getTopK() : 4;
        String query = planText.isBlank() ? "판매자 사업계획서 심사 기준" : trimQuery(planText);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    // Trim long text to a reasonable query size for retrieval.
    private String trimQuery(String planText) {
        int limit = 2000;
        return planText.length() > limit ? planText.substring(0, limit) : planText;
    }

    // Normalize null strings for prompt formatting.
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private EvaluateDTO requestEvaluation(SystemMessage systemMessage, UserMessage userMessage) {
        String answer;
        try {
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(MODEL)
                    .inputOfResponse(buildResponseInput(List.of(systemMessage, userMessage)))
                    .temperature(0.2)
                    .build();
            Response response = openAIClient.responses().create(params);
            answer = extractOutputText(response);
        } catch (OpenAIException e) {
            throw new IllegalStateException("ai evaluation failed", e);
        }

        String payload = normalizeJson(answer);
        return toEvaluateDTO(payload);
    }

    private EvaluateDTO toEvaluateDTO(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            int businessStability = node.path("businessStability").asInt();
            int productCompetency = node.path("productCompetency").asInt();
            int liveSuitability = node.path("liveSuitability").asInt();
            int operationCoop = node.path("operationCoop").asInt();
            int growthPotential = node.path("growthPotential").asInt();
            int totalScore = node.path("totalScore").asInt();
            String grade = node.path("gradeRecommended").asText();
            String summary = node.path("summary").asText("");

            return chatTools.getEvaluateResultTool(
                    businessStability,
                    productCompetency,
                    liveSuitability,
                    operationCoop,
                    growthPotential,
                    totalScore,
                    com.deskit.deskit.account.enums.SellerGradeEnum.valueOf(grade),
                    summary
            );
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("invalid evaluation payload", ex);
        }
    }

    private List<ResponseInputItem> buildResponseInput(List<org.springframework.ai.chat.messages.Message> messages) {
        List<ResponseInputItem> inputItems = new ArrayList<>();
        for (org.springframework.ai.chat.messages.Message message : messages) {
            EasyInputMessage easyMessage = EasyInputMessage.builder()
                    .role(toEasyInputRole(message))
                    .content(getMessageText(message))
                    .build();
            inputItems.add(ResponseInputItem.ofEasyInputMessage(easyMessage));
        }
        return inputItems;
    }

    private EasyInputMessage.Role toEasyInputRole(org.springframework.ai.chat.messages.Message message) {
        return switch (message.getMessageType()) {
            case SYSTEM -> EasyInputMessage.Role.SYSTEM;
            case USER -> EasyInputMessage.Role.USER;
            case ASSISTANT -> EasyInputMessage.Role.ASSISTANT;
            case TOOL -> EasyInputMessage.Role.SYSTEM;
        };
    }

    private String getMessageText(org.springframework.ai.chat.messages.Message message) {
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.getText();
        }
        if (message instanceof UserMessage userMessage) {
            return userMessage.getText();
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

    private String normalizeJson(String answer) {
        if (answer == null) {
            return "";
        }
        String trimmed = answer.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }
}
