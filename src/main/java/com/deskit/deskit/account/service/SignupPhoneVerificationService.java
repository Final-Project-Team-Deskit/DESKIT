package com.deskit.deskit.account.service;

import com.deskit.deskit.common.util.verification.SmsVerificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SignupPhoneVerificationService {

    private final SmsVerificationService smsVerificationService;

    public String sendCode(String phoneNumber, HttpSession session) {
        if (session == null || phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        String code = generateCode();
        session.setAttribute(SignupPhoneSessionKeys.SESSION_PHONE_NUMBER, phoneNumber);
        session.setAttribute(SignupPhoneSessionKeys.SESSION_PHONE_CODE, code);
        session.setAttribute(SignupPhoneSessionKeys.SESSION_PHONE_VERIFIED, false);

        boolean sent = smsVerificationService.sendVerificationCode(phoneNumber, code);
        if (!sent) {
            session.removeAttribute(SignupPhoneSessionKeys.SESSION_PHONE_CODE);
            session.setAttribute(SignupPhoneSessionKeys.SESSION_PHONE_VERIFIED, false);
            return null;
        }

        return code;
    }

    public boolean verifyCode(String phoneNumber, String code, HttpSession session) {
        if (session == null || phoneNumber == null || code == null) {
            return false;
        }

        String storedPhone = (String) session.getAttribute(SignupPhoneSessionKeys.SESSION_PHONE_NUMBER);
        String storedCode = (String) session.getAttribute(SignupPhoneSessionKeys.SESSION_PHONE_CODE);

        if (!Objects.equals(phoneNumber, storedPhone) || !Objects.equals(code, storedCode)) {
            return false;
        }

        session.setAttribute(SignupPhoneSessionKeys.SESSION_PHONE_VERIFIED, true);
        return true;
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
