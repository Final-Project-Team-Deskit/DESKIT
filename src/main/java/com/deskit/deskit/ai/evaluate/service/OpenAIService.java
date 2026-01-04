package com.deskit.deskit.ai.evaluate.service;

import com.deskit.deskit.ai.evaluate.dto.AiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAiChatModel openAiChatModel;

    // Chat 모델
    public AiResponse generate(String text) {

        ChatClient chatClient = ChatClient.create(openAiChatModel);

        // 메시지
        // SystemMessage = 프롬프팅할 내용
        SystemMessage systemMessage = new SystemMessage("""
                당신은 우리 DESKIT 플랫폼에 회원가입을 신청한 판매자(사업자)들의 사업계획서를 심사하는 AI 심사 봇입니다.
                사업계획서를 분석하고, 제공된 문서를 바탕으로 사업계획서를 심사한 결과를 제시하세요.
                
                결과를 제시할 때, 결과 요약(summary) 필드에는 심사 결과에 대한 이유를 간단히 작성하세요.
                
                

                """);
        // UserMessage = 사용자의 질문
        UserMessage userMessage = new UserMessage(text);
        // AssistantMessage = AI의 답변
        AssistantMessage assistantMessage = new AssistantMessage("");

        // 옵션
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .build();

        // 프롬프트
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);

        // LLM 호출
        AiResponse response = chatClient.prompt(prompt)
                .tools(new AiTools())
                .call()
                .entity(AiResponse.class);

        return response;
    }
}
