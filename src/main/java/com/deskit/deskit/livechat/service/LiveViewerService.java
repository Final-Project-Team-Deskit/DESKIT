package com.deskit.deskit.livechat.service;

import com.deskit.deskit.livechat.dto.LiveViewerCountDTO;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LiveViewerService {
    private final SimpMessageSendingOperations messagingTemplate;
    private final Map<String, Long> sessionToBroadcast = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> viewerCounts = new ConcurrentHashMap<>();

    public LiveViewerService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void registerViewer(String sessionId, Long broadcastId, String role) {
        if (sessionId == null || broadcastId == null || !shouldCount(role)) {
            return;
        }
        synchronized (this) {
            Long previous = sessionToBroadcast.put(sessionId, broadcastId);
            if (previous != null && previous.equals(broadcastId)) {
                return;
            }
            if (previous != null) {
                decrement(previous);
            }
            increment(broadcastId);
        }
    }

    public void unregisterViewer(String sessionId) {
        if (sessionId == null) {
            return;
        }
        synchronized (this) {
            Long broadcastId = sessionToBroadcast.remove(sessionId);
            if (broadcastId == null) {
                return;
            }
            decrement(broadcastId);
        }
    }

    private void increment(Long broadcastId) {
        int count = viewerCounts.computeIfAbsent(broadcastId, key -> new AtomicInteger()).incrementAndGet();
        broadcastCount(broadcastId, count);
    }

    private void decrement(Long broadcastId) {
        AtomicInteger counter = viewerCounts.get(broadcastId);
        if (counter == null) {
            return;
        }
        int count = counter.decrementAndGet();
        if (count <= 0) {
            viewerCounts.remove(broadcastId);
            count = 0;
        }
        broadcastCount(broadcastId, count);
    }

    private void broadcastCount(Long broadcastId, int count) {
        messagingTemplate.convertAndSend(
                "/sub/live/" + broadcastId + "/viewers",
                new LiveViewerCountDTO(broadcastId, count)
        );
    }

    private boolean shouldCount(String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        return !(role.startsWith("ROLE_ADMIN") || role.startsWith("ROLE_SELLER"));
    }
}
