package com.deskit.deskit.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지를 구독(수신)하는 요청의 접두사
        config.enableSimpleBroker("/topic", "/queue");

        // 메시지를 발행(송신)하는 요청의 접두사
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트엔드에서 연결할 웹소켓 엔드포인트 주소
        registry.addEndpoint("/ws-live")
                // CORS 허용 (프론트엔드 포트가 5173인 경우)
                .setAllowedOriginPatterns("http://localhost:5173", "http://localhost:3000", "*")
                .withSockJS(); // SockJS 지원
    }
}
