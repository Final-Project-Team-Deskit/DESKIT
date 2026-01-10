package com.deskit.deskit.common.util.verification;

import com.deskit.deskit.common.util.verification.solapi.SolapiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SmsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsVerificationService.class);
    private static final String MESSAGE_TEMPLATE = "[DESKIT] 요청하신 인증번호는 %s 입니다.";

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiSecret;
    private final String fromNumber;

    public SmsVerificationService(
            ObjectMapper objectMapper,
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.from-number:}") String fromNumber
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.fromNumber = fromNumber;
    }

    public boolean sendVerificationCode(String phoneNumber, String code) {
        if (phoneNumber == null || phoneNumber.isBlank() || code == null || code.isBlank()) {
            return false;
        }

        if (apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()
                || fromNumber == null || fromNumber.isBlank()) {
            log.warn("Solapi credentials are not configured.");
            return false;
        }

        String messageText = MESSAGE_TEMPLATE.formatted(code);
        String payload;
        try {
            payload = buildPayload(phoneNumber, messageText);
        } catch (JsonProcessingException ex) {
            log.error("Failed to build Solapi payload: {}", ex.getMessage());
            return false;
        }

        try {
            String response = SolapiClient.sendMessage(apiKey, apiSecret, payload);
            log.info("Solapi response: {}", response);
            return true;
        } catch (Exception ex) {
            log.error("Solapi send failed: {}", ex.getMessage());
            return false;
        }
    }

    private String buildPayload(String phoneNumber, String messageText) throws JsonProcessingException {
        Map<String, Object> message = Map.of(
                "to", phoneNumber,
                "from", fromNumber,
                "text", messageText
        );
        Map<String, Object> payload = Map.of("messages", List.of(message));
        return objectMapper.writeValueAsString(payload);
    }
}
