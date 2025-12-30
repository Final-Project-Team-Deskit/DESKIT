package com.deskit.deskit.account.controller;

import com.deskit.deskit.account.dto.SocialSignupRequest;
import com.deskit.deskit.account.entity.*;
import com.deskit.deskit.account.enums.*;
import com.deskit.deskit.account.jwt.JWTUtil;
import com.deskit.deskit.account.oauth.CustomOAuth2User;
import com.deskit.deskit.account.repository.*;
import com.deskit.deskit.common.util.verification.PhoneSendRequest;
import com.deskit.deskit.common.util.verification.PhoneSendResponse;
import com.deskit.deskit.common.util.verification.PhoneVerifyRequest;
import com.deskit.deskit.account.dto.PendingSignupResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/signup/social")
public class SignupController {

    // Session key for phone number awaiting verification.
    private static final String SESSION_PHONE_NUMBER = "pendingPhoneNumber";

    // Session key for verification code.
    private static final String SESSION_PHONE_CODE = "pendingPhoneCode";

    // Session key for verification completion flag.
    private static final String SESSION_PHONE_VERIFIED = "pendingPhoneVerified";

    private final MemberRepository memberRepository;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final SellerRepository sellerRepository;
    private final CompanyRegisteredRepository companyRegisteredRepository;
    private final SellerRegisterRepository sellerRegisterRepository;
    private final SellerGradeRepository sellerGradeRepository;
    private final InvitationRepository invitationRepository;

    // Provide pending signup info to the frontend after social login.
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        // Reject if unauthenticated.
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // Reject if signup is already completed.
        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Response payload to prefill signup form.
        PendingSignupResponse response = PendingSignupResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Send a verification code for phone validation.
    @PostMapping("/phone/send")
    public ResponseEntity<?> sendPhoneCode(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody PhoneSendRequest request,
            HttpSession session
    ) {
        // Reject if unauthenticated.
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // Reject if signup is already completed.
        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Phone number from the request payload.
        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new ResponseEntity<>("phone number required", HttpStatus.BAD_REQUEST);
        }

        // Generate a 6-digit verification code for development.
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));

        // Persist phone verification state in session.
        session.setAttribute(SESSION_PHONE_NUMBER, phoneNumber);
        session.setAttribute(SESSION_PHONE_CODE, code);
        session.setAttribute(SESSION_PHONE_VERIFIED, false);

        // Response payload containing dev-only code.
        PhoneSendResponse response = PhoneSendResponse.builder()
                .message("verification code generated")
                .code(code)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Verify phone code submitted by the user.
    @PostMapping("/phone/verify")
    public ResponseEntity<?> verifyPhoneCode(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody PhoneVerifyRequest request,
            HttpSession session
    ) {
        // Reject if unauthenticated.
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // Reject if signup is already completed.
        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Phone number and code from the request payload.
        String phoneNumber = request.getPhoneNumber();
        String code = request.getCode();

        // Session-stored verification state.
        String storedPhone = (String) session.getAttribute(SESSION_PHONE_NUMBER);
        String storedCode = (String) session.getAttribute(SESSION_PHONE_CODE);

        if (!Objects.equals(phoneNumber, storedPhone) || !Objects.equals(code, storedCode)) {
            return new ResponseEntity<>("verification failed", HttpStatus.BAD_REQUEST);
        }

        session.setAttribute(SESSION_PHONE_VERIFIED, true);

        return new ResponseEntity<>("verified", HttpStatus.OK);
    }

    // Complete signup for general members after phone verification.
    @PostMapping("/complete")
    @Transactional
    public ResponseEntity<?> completeSignup(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        // Reject if unauthenticated.
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // Reject if signup is already completed.
        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Ensure user does not already exist in DB.
        Member existData = memberRepository.findByLoginId(user.getEmail());
        if (existData != null) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Ensure seller does not already exist in DB.
        Seller existSeller = sellerRepository.findByLoginId(user.getEmail());
        if (existSeller != null) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Validate terms agreement.
        if (!request.isAgreed()) {
            return new ResponseEntity<>("terms agreement required", HttpStatus.BAD_REQUEST);
        }

        // Ensure phone verification completed.
        Boolean verified = (Boolean) session.getAttribute(SESSION_PHONE_VERIFIED);
        if (verified == null || !verified) {
            return new ResponseEntity<>("phone verification required", HttpStatus.BAD_REQUEST);
        }

        // Validate verified phone number matches request.
        String storedPhone = (String) session.getAttribute(SESSION_PHONE_NUMBER);
        if (!Objects.equals(storedPhone, request.getPhoneNumber())) {
            return new ResponseEntity<>("phone number mismatch", HttpStatus.BAD_REQUEST);
        }

        // Member type selected for signup branching.
        // Raw member type input from request payload.
        String memberTypeRaw = trimToNull(request.getMemberType());
        if (memberTypeRaw == null) {
            return new ResponseEntity<>("member type required", HttpStatus.BAD_REQUEST);
        }

        // Normalized member type for signup branching.
        String memberType = normalizeMemberType(memberTypeRaw);
        if (memberType == null) {
            return new ResponseEntity<>("unsupported member type", HttpStatus.BAD_REQUEST);
        }

        if ("GENERAL".equals(memberType)) {
            return completeGeneralSignup(user, request, response, session, storedPhone);
        }

        if ("SELLER".equals(memberType)) {
            return completeSellerSignup(user, request, response, session, storedPhone);
        }

        return new ResponseEntity<>("unsupported member type", HttpStatus.BAD_REQUEST);
    }

    // Handle general member signup completion.
    private ResponseEntity<?> completeGeneralSignup(
            CustomOAuth2User user,
            SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session,
            String storedPhone
    ) {
        // Build and persist the new user entity.
        Member member = Member.builder()
                .loginId(user.getEmail())
                .name(user.getName())
                .role("ROLE_MEMBER")
                .phone(storedPhone)
                .profile(user.getProfileUrl())
                .status(MemberStatus.ACTIVE)
                .mbti(MBTI.valueOf(trimToNull(String.valueOf(request.getMbti()))))
                .jobCategory(JobCategory.valueOf(trimToNull(String.valueOf(request.getJobCategory()))))
                .isAgreed(request.isAgreed())
                .build();

        memberRepository.save(member);

        // Issue tokens after successful signup.
        issueTokens(member.getLoginId(), member.getRole(), response);

        // Clear phone verification state after completion.
        clearPhoneSession(session);

        return new ResponseEntity<>("signup completed", HttpStatus.OK);
    }

    // Handle seller signup completion with review data.
    private ResponseEntity<?> completeSellerSignup(
            CustomOAuth2User user,
            SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session,
            String storedPhone
    ) {
        // Invitation token for invited seller signup.
        String inviteToken = trimToNull(request.getInviteToken());
        if (inviteToken != null) {
            return completeInvitedSellerSignup(user, response, session, storedPhone, inviteToken);
        }

        // Business number required for seller registration.
        String businessNumber = trimToNull(request.getBusinessNumber());
        if (businessNumber == null) {
            return new ResponseEntity<>("business number required", HttpStatus.BAD_REQUEST);
        }

        // Company name required for seller registration.
        String companyName = trimToNull(request.getCompanyName());
        if (companyName == null) {
            return new ResponseEntity<>("company name required", HttpStatus.BAD_REQUEST);
        }

        // Plan file payload required for seller registration.
        String planFileBase64 = trimToNull(request.getPlanFileBase64());
        if (planFileBase64 == null) {
            return new ResponseEntity<>("plan file required", HttpStatus.BAD_REQUEST);
        }

        // Reject when the business number is already registered.
        CompanyRegistered existingCompany = companyRegisteredRepository.findByBusinessNumber(businessNumber);
        if (existingCompany != null
                && CompanyStatus.ACTIVE.name().equalsIgnoreCase(existingCompany.getCompanyStatus().toString())) {
            return new ResponseEntity<>("business number already registered", HttpStatus.CONFLICT);
        }

        // Optional seller description for review.
        String description = trimToNull(request.getDescription());

        // Decode plan file from base64 payload.
        byte[] planFile;
        try {
            planFile = decodePlanFile(planFileBase64);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>("invalid plan file payload", HttpStatus.BAD_REQUEST);
        }

        // Timestamp used for seller-related records.
        LocalDateTime now = LocalDateTime.now();

        // Build and persist the new seller user entity.
        Seller seller = Seller.builder()
                .loginId(user.getEmail())
                .name(user.getName())
                .phone(storedPhone)
                .profile(user.getProfileUrl())
                .role(SellerRole.valueOf(SellerRole.ROLE_SELLER_OWNER.name()))
                .status(SellerStatus.valueOf(SellerStatus.PENDING.name()))
                .createdAt(now)
                .updatedAt(now)
                .isAgreed(request.isAgreed())
                .build();

        sellerRepository.save(seller);

        // Store the registered company for duplicate checks.
        CompanyRegistered companyRegistered = CompanyRegistered.builder()
                .companyName(companyName)
                .businessNumber(businessNumber)
                .sellerId(seller.getSellerId())
                .createdAt(now)
                .companyStatus(CompanyStatus.valueOf(CompanyStatus.ACTIVE.name()))
                .build();

        companyRegisteredRepository.save(companyRegistered);

        // Store seller review submission details.
        SellerRegister sellerRegister = SellerRegister.builder()
                .planFile(planFile)
                .sellerId(seller.getSellerId())
                .description(description)
                .companyName(companyName)
                .build();

        sellerRegisterRepository.save(sellerRegister);

        // Assign initial seller grade in review status.
        SellerGrade sellerGrade = SellerGrade.builder()
                .grade(SellerGradeEnum.valueOf(SellerGradeEnum.C.name()))
                .gradeStatus(SellerGradeStatus.valueOf(SellerGradeStatus.REVIEW.name()))
                .createdAt(now)
                .updatedAt(now)
                .expiredAt(now.plusYears(1))
                .companyId(companyRegistered.getCompanyId())
                .build();

        sellerGradeRepository.save(sellerGrade);

        // Issue tokens after successful signup request.
        issueTokens(seller.getLoginId(), String.valueOf(seller.getRole()), response);

        // Clear phone verification state after completion.
        clearPhoneSession(session);

        return new ResponseEntity<>(
                "판매자 회원 가입 신청이 완료되었습니다. 관리자 승인 후에 서비스 이용이 가능합니다.",
                HttpStatus.OK
        );
    }

    // Handle invited seller signup completion with manager role.
    private ResponseEntity<?> completeInvitedSellerSignup(
            CustomOAuth2User user,
            HttpServletResponse response,
            HttpSession session,
            String storedPhone,
            String inviteToken
    ) {
        // Invitation lookup by token.
        Invitation invitation = invitationRepository.findByToken(inviteToken);
        if (invitation == null) {
            return new ResponseEntity<>("invitation not found", HttpStatus.NOT_FOUND);
        }

        // Validate invitation status.
        if (!InvitationStatus.PENDING.name().equalsIgnoreCase(String.valueOf(invitation.getStatus()))) {
            return new ResponseEntity<>("invitation already used", HttpStatus.CONFLICT);
        }

        // Validate invitation expiration.
        LocalDateTime now = LocalDateTime.now();
        if (invitation.getExpiredAt() != null && invitation.getExpiredAt().isBefore(now)) {
            invitation.setStatus(InvitationStatus.valueOf(InvitationStatus.EXPIRED.name()));
            invitation.setUpdatedAt(now);
            invitationRepository.save(invitation);
            return new ResponseEntity<>("invitation expired", HttpStatus.GONE);
        }

        // Ensure the invitation email matches the signup email.
        String inviteEmail = trimToNull(invitation.getEmail());
        String signupEmail = trimToNull(user.getEmail());
        if (inviteEmail == null || signupEmail == null || !inviteEmail.equalsIgnoreCase(signupEmail)) {
            return new ResponseEntity<>("invitation email mismatch", HttpStatus.BAD_REQUEST);
        }

        // Ensure the invitation owner seller exists.
        Seller ownerSeller = sellerRepository.findById(invitation.getSellerId()).orElse(null);
        if (ownerSeller == null) {
            return new ResponseEntity<>("invitation owner not found", HttpStatus.NOT_FOUND);
        }

        // Build and persist the new manager seller entity.
        Seller seller = Seller.builder()
                .loginId(user.getEmail())
                .name(user.getName())
                .phone(storedPhone)
                .profile(user.getProfileUrl())
                .role(SellerRole.valueOf(SellerRole.ROLE_SELLER_MANAGER.name()))
                .status(SellerStatus.valueOf(SellerStatus.ACTIVE.name()))
                .createdAt(now)
                .updatedAt(now)
                .isAgreed(true)
                .build();

        sellerRepository.save(seller);

        // Mark invitation as accepted after successful signup.
        invitation.setStatus(InvitationStatus.valueOf(InvitationStatus.ACCEPTED.name()));
        invitation.setUpdatedAt(now);
        invitationRepository.save(invitation);

        // Issue tokens after successful invited signup.
        issueTokens(seller.getLoginId(), String.valueOf(seller.getRole()), response);

        // Clear phone verification state after completion.
        clearPhoneSession(session);

        return new ResponseEntity<>("invited seller signup completed", HttpStatus.OK);
    }

    // Issue access/refresh tokens and persist refresh token.
    private void issueTokens(String username, String role, HttpServletResponse response) {
        // Access token expiry in milliseconds.
        long accessExpiryMs = 600000L;

        // Refresh token expiry in milliseconds.
        long refreshExpiryMs = 86400000L;

        // Create tokens for the signed-up user.
        String access = jwtUtil.createJwt("access", username, role, accessExpiryMs); // Access token for header.
        String refresh = jwtUtil.createJwt("refresh", username, role, refreshExpiryMs); // Refresh token for cookie.

        refreshRepository.save(username, refresh, refreshExpiryMs);

        response.setHeader("access", access);
        response.addCookie(createCookie("access", access, Math.toIntExact(accessExpiryMs / 1000)));
        response.addCookie(createCookie("refresh", refresh, Math.toIntExact(refreshExpiryMs / 1000)));
    }

    // Create a cookie with HttpOnly flag.
    private Cookie createCookie(String key, String value, int maxAge) {
        // Cookie for refresh token storage.
        Cookie cookie = new Cookie(key, value);

        cookie.setMaxAge(maxAge);
        // cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

    // Persist refresh token record for reissue workflow.
    // Clear phone verification attributes after signup completion.
    private void clearPhoneSession(HttpSession session) {
        // Cleanup pending phone state stored in session.
        session.removeAttribute(SESSION_PHONE_NUMBER);
        session.removeAttribute(SESSION_PHONE_CODE);
        session.removeAttribute(SESSION_PHONE_VERIFIED);
    }

    // Decode base64 plan file payload, tolerating data URL prefixes.
    private byte[] decodePlanFile(String encodedPlanFile) {
        // Remove data URL prefix if present.
        int commaIndex = encodedPlanFile.indexOf(',');
        String payload = commaIndex >= 0 ? encodedPlanFile.substring(commaIndex + 1) : encodedPlanFile;
        return Base64.getDecoder().decode(payload);
    }

    // Normalize optional text input to null when blank.
    private String trimToNull(String value) {
        // Trimmed value from optional input.
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    // Normalize member type input for routing.
    private String normalizeMemberType(String memberType) {
        // Upper-cased member type value for validation.
        String normalized = memberType == null ? null : memberType.trim().toUpperCase();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if ("GENERAL".equals(normalized) || "SELLER".equals(normalized)) {
            return normalized;
        }
        return null;
    }
}



