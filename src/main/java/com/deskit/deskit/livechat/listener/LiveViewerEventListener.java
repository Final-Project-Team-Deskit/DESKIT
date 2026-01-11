package com.deskit.deskit.livechat.listener;

import com.deskit.deskit.livechat.service.LiveViewerService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LiveViewerEventListener {
    private static final Pattern CHAT_SUBSCRIBE_PATTERN = Pattern.compile("^/sub/chat/(\\d+)$");

    private final LiveViewerService liveViewerService;

    public LiveViewerEventListener(LiveViewerService liveViewerService) {
        this.liveViewerService = liveViewerService;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Long broadcastId = extractBroadcastId(destination);
        String sessionId = accessor.getSessionId();
        if (broadcastId == null || sessionId == null) {
            return;
        }
        String role = resolveRole(accessor);
        liveViewerService.registerViewer(sessionId, broadcastId, role);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            liveViewerService.unregisterViewer(sessionId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            liveViewerService.unregisterViewer(sessionId);
        }
    }

    private Long extractBroadcastId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = CHAT_SUBSCRIBE_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveRole(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) {
            return null;
        }
        Object role = accessor.getSessionAttributes().get("role");
        return role instanceof String ? (String) role : null;
    }
}
