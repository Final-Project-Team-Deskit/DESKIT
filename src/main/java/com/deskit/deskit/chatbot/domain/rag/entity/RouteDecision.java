package com.deskit.deskit.chatbot.domain.rag.entity;

public record RouteDecision(ChatRoute route, int score, String reason) {
}
