package com.deskit.deskit.ai.chatbot.rag.service;

import com.deskit.deskit.ai.chatbot.rag.entity.ChatRoute;
import com.deskit.deskit.ai.chatbot.rag.entity.RouteDecision;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class ChatRoutingService {

    // RAG에 강하게 매핑되는 키워드
    private static final Map<String, Integer> RAG_KEYWORDS = Map.ofEntries(
            Map.entry("관리자", 3),
            Map.entry("환불", 3),
            Map.entry("취소", 3),
            Map.entry("반품", 3),
            Map.entry("교환", 3),
            Map.entry("정책", 3),
            Map.entry("접수", 3),
            Map.entry("가격", 2),
            Map.entry("확인", 2),
            Map.entry("판매자", 2),
            Map.entry("결제", 2),
            Map.entry("정산", 2),
            Map.entry("배송", 2),
            Map.entry("공지", 1)
    );

    // RAG로 보내기 전에 최소 길이 확인
    private static final int MIN_LEN_FOR_RAG = 6;

    // 점수가 이 값 이상이면 RAG로 보냄
    private static final int RAG_THRESHOLD = 3;

    public RouteDecision decide(String question) {
        String q = normalize(question);
        log.info(q);

        if (q == null || q.isBlank()) {
            log.info("q is blank");
            return new RouteDecision(ChatRoute.GENERAL, 0, "empty");
        }

        if (q.length() < MIN_LEN_FOR_RAG) {
            log.info("q is too short");
            return new RouteDecision(ChatRoute.GENERAL, 0, "too_short");
        }

        int score = 0;
        for (var e : RAG_KEYWORDS.entrySet()) {
            log.info("entrySet : " + e.getKey());
            if (q.contains(e.getKey())) score += e.getValue();
        }

        if (looksLikePolicyQuestion(q)) score += 1;

        ChatRoute route = (score >= RAG_THRESHOLD) ? ChatRoute.RAG : ChatRoute.GENERAL;
        log.info(route);
        return new RouteDecision(route, score, "keyword_score");
    }

    private boolean looksLikePolicyQuestion(String q) {
        return q.contains("가격") || q.contains("왜") || q.contains("기준") || q.contains("조건");
    }

    private String normalize(String q) {
        if (q == null) return null;
        return q.trim();
    }
}
