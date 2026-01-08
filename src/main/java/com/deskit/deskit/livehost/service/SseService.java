package com.deskit.deskit.livehost.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long broadcastId, String userId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        String key = broadcastId + "_" + userId;

        emitters.put(key, emitter);

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitter.onError((e) -> emitters.remove(key));

        sendToClient(emitter, key, "connect", "Connected!");

        return emitter;
    }

    public void notifyBroadcastUpdate(Long broadcastId, String eventName, Object data) {
        emitters.forEach((key, emitter) -> {
            if (key.startsWith(broadcastId + "_")) {
                sendToClient(emitter, key, eventName, data);
            }
        });
    }

    public void notifyBroadcastUpdate(Long broadcastId, String eventName) {
        notifyBroadcastUpdate(broadcastId, eventName, "update");
    }

    public void notifyBroadcastUpdate(Long broadcastId) {
        notifyBroadcastUpdate(broadcastId, "BROADCAST_UPDATED", "info_changed");
    }

    public void notifyTargetUser(Long broadcastId, Long userId, String eventName, Object data) {
        String key = broadcastId + "_" + userId;
        SseEmitter emitter = emitters.get(key);

        if (emitter != null) {
            sendToClient(emitter, key, eventName, data);
        } else {
            log.warn("Target user not found or disconnected: key={}", key);
        }
    }

    private void sendToClient(SseEmitter emitter, String id, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().id(id).name(name).data(data));
        } catch (IOException e) {
            emitters.remove(id);
        }
    }
}
