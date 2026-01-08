package com.deskit.deskit.livehost.service;

import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenViduService {

    private final OpenVidu openVidu;

    private final Map<Long, String> sessionMap = new ConcurrentHashMap<>();

    public String createSession(Long broadcastId) throws OpenViduJavaClientException, OpenViduHttpException {
        if (sessionMap.containsKey(broadcastId)) {
            return sessionMap.get(broadcastId);
        }

        RecordingProperties recordingProperties = new RecordingProperties.Builder()
                .outputMode(Recording.OutputMode.COMPOSED)
                .build();

        SessionProperties properties = new SessionProperties.Builder()
                .customSessionId("broadcast-" + broadcastId)
                .recordingMode(RecordingMode.MANUAL)
                .defaultRecordingProperties(recordingProperties)
                .build();

        Session session = openVidu.createSession(properties);
        sessionMap.put(broadcastId, session.getSessionId());

        log.info("OpenVidu 세션 생성: broadcastId={}, sessionId={}", broadcastId, session.getSessionId());
        return session.getSessionId();
    }

    public String createToken(Long broadcastId, Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {

        String sessionId = sessionMap.get(broadcastId);
        if (sessionId == null) {
            sessionId = createSession(broadcastId);
        }

        Session session = openVidu.getActiveSession(sessionId);
        if (session == null) {
            sessionId = createSession(broadcastId);
            session = openVidu.getActiveSession(sessionId);
        }

        OpenViduRole role = OpenViduRole.PUBLISHER;
        if (params != null && params.containsKey("role")) {
            String requestedRole = String.valueOf(params.get("role"));
            if ("HOST".equalsIgnoreCase(requestedRole)) {
                role = OpenViduRole.PUBLISHER;
            } else {
                role = OpenViduRole.valueOf(requestedRole.toUpperCase());
            }
        }

        ConnectionProperties properties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(role)
                .data(params != null ? params.toString() : "")
                .build();

        Connection connection = session.createConnection(properties);
        return connection.getToken();
    }

    public void startRecording(Long broadcastId) throws OpenViduJavaClientException, OpenViduHttpException {
        openVidu.startRecording(sessionMap.get(broadcastId));
    }

    public void stopRecording(Long broadcastId) throws OpenViduJavaClientException, OpenViduHttpException {
        String sessionId = sessionMap.get(broadcastId);
        if (sessionId != null) {
            openVidu.stopRecording(sessionId);
        }
    }

    public void closeSession(Long broadcastId) {
        String sessionId = sessionMap.remove(broadcastId);
        if (sessionId != null) {
            try {
                Session session = openVidu.getActiveSession(sessionId);
                if (session != null) {
                    session.close();
                    log.info("OpenVidu 세션 종료: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("세션 종료 중 오류: {}", e.getMessage());
            }
        }
    }

    public void forceDisconnect(Long broadcastId, String connectionId) {
        String sessionId = sessionMap.get(broadcastId);
        if (sessionId == null) {
            return;
        }

        try {
            Session session = openVidu.getActiveSession(sessionId);
            if (session != null) {
                session.forceDisconnect(connectionId);
                log.info("Force disconnected connection: {}", connectionId);
            }
        } catch (Exception e) {
            log.error("Failed to force disconnect: {}", e.getMessage());
        }
    }
}
