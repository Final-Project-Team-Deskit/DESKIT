package com.deskit.deskit.account.controller;

import com.deskit.deskit.account.dto.SocialSignupRequest;
import com.deskit.deskit.account.entity.*;
import com.deskit.deskit.account.enums.*;
import com.deskit.deskit.account.oauth.CustomOAuth2User;
import com.deskit.deskit.account.repository.*;
import com.deskit.deskit.common.util.verification.PhoneSendRequest;
import com.deskit.deskit.common.util.verification.PhoneSendResponse;
import com.deskit.deskit.common.util.verification.PhoneVerifyRequest;
import com.deskit.deskit.ai.evaluate.service.SellerPlanEvaluationService;
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

    // 세션 키 값 : 인증받을 폰 번호
    private static final String SESSION_PHONE_NUMBER = "pendingPhoneNumber";

    // 세션 키 값 : 인증코드
    private static final String SESSION_PHONE_CODE = "pendingPhoneCode";

    // 세션 키 값 : 인증 확인 여부
    private static final String SESSION_PHONE_VERIFIED = "pendingPhoneVerified";

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;
    private final CompanyRegisteredRepository companyRegisteredRepository;
    private final SellerRegisterRepository sellerRegisterRepository;
    private final SellerGradeRepository sellerGradeRepository;
    private final InvitationRepository invitationRepository;
    private final SellerPlanEvaluationService sellerPlanEvaluationService;

    // 소셜 로그인 후 추가 정보 입력 후 회원 가입 진행 - 대기중인 상태
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        PendingSignupResponse response = PendingSignupResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 전화번호 인증을 위한 인증코드 발송
    @PostMapping("/phone/send")
    public ResponseEntity<?> sendPhoneCode(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody PhoneSendRequest request,
            HttpSession session
    ) {
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // request payload에서 번호 꺼내기
        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new ResponseEntity<>("phone number required", HttpStatus.BAD_REQUEST);
        }

        // 개발용 인증 코드 6자리 생성
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 세션에 인증 관련 정보 담기
        session.setAttribute(SESSION_PHONE_NUMBER, phoneNumber);
        session.setAttribute(SESSION_PHONE_CODE, code);
        session.setAttribute(SESSION_PHONE_VERIFIED, false);

        // 개발용 인증 코드 담은 Response payload
        PhoneSendResponse response = PhoneSendResponse.builder()
                .message("verification code generated")
                .code(code)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 인증 번호 확인
    @PostMapping("/phone/verify")
    public ResponseEntity<?> verifyPhoneCode(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody PhoneVerifyRequest request,
            HttpSession session
    ) {
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // request payload에서 번호와 코드 꺼내기
        String phoneNumber = request.getPhoneNumber();
        String code = request.getCode();

        // 세션에 저장된 번호와 코드 꺼내기
        String storedPhone = (String) session.getAttribute(SESSION_PHONE_NUMBER);
        String storedCode = (String) session.getAttribute(SESSION_PHONE_CODE);

        if (!Objects.equals(phoneNumber, storedPhone) || !Objects.equals(code, storedCode)) {
            return new ResponseEntity<>("verification failed", HttpStatus.BAD_REQUEST);
        }

        session.setAttribute(SESSION_PHONE_VERIFIED, true);

        return new ResponseEntity<>("verified", HttpStatus.OK);
    }

    // 일반 회원 추가 정보 입력 진행
    @PostMapping("/complete")
    @Transactional
    public ResponseEntity<?> completeSignup(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        if (user == null) {
            return new ResponseEntity<>("unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isNewUser()) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Member에 있는 정보인지 조회
        Member existMember = memberRepository.findByLoginId(user.getEmail());
        if (existMember != null) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        // Seller에 있는 정보인지 조회
        Seller existSeller = sellerRepository.findByLoginId(user.getEmail());
        if (existSeller != null) {
            return new ResponseEntity<>("already signed up", HttpStatus.CONFLICT);
        }

        Boolean verified = (Boolean) session.getAttribute(SESSION_PHONE_VERIFIED);
        if (verified == null || !verified) {
            return new ResponseEntity<>("phone verification required", HttpStatus.BAD_REQUEST);
        }

        String storedPhone = (String) session.getAttribute(SESSION_PHONE_NUMBER);
        if (!Objects.equals(storedPhone, request.getPhoneNumber())) {
            return new ResponseEntity<>("phone number mismatch", HttpStatus.BAD_REQUEST);
        }

        // memberType null 배제
        String memberTypeRaw = trimToNull(request.getMemberType());
        if (memberTypeRaw == null) {
            return new ResponseEntity<>("member type required", HttpStatus.BAD_REQUEST);
        }

        String memberType = normalizeMemberType(memberTypeRaw);
        if (memberType == null) {
            return new ResponseEntity<>("unsupported member type", HttpStatus.BAD_REQUEST);
        }

        // GENERAL : 일반 회원, SELLER : 판매자 (내부적으로 활용)
        if ("GENERAL".equals(memberType)) {
            return completeGeneralSignup(user, request, response, session, storedPhone);
        }

        if ("SELLER".equals(memberType)) {
            return completeSellerSignup(user, request, response, session, storedPhone);
        }

        return new ResponseEntity<>("unsupported member type", HttpStatus.BAD_REQUEST);
    }

    // 일반 회원 가입 완성
    private ResponseEntity<?> completeGeneralSignup(
            CustomOAuth2User user,
            SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session,
            String storedPhone
    ) {
        // 객체 조립
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

        clearAuthCookies(response);

        // 폰 인증 세션 삭제
        clearPhoneSession(session);

        return new ResponseEntity<>("signup completed", HttpStatus.OK);
    }

    // 판매자 회원 가입 완성
    private ResponseEntity<?> completeSellerSignup(
            CustomOAuth2User user,
            SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session,
            String storedPhone
    ) {
        // 초대 토큰 검증
        String inviteToken = trimToNull(request.getInviteToken());
        if (inviteToken != null) {
            return completeInvitedSellerSignup(user, request, response, session, storedPhone, inviteToken);
        }

        // 사업자등록번호 검증 - null 배제
        String businessNumber = trimToNull(request.getBusinessNumber());
        if (businessNumber == null) {
            return new ResponseEntity<>("business number required", HttpStatus.BAD_REQUEST);
        }

        // 사업자명 검증 - null 배제
        String companyName = trimToNull(request.getCompanyName());
        if (companyName == null) {
            return new ResponseEntity<>("company name required", HttpStatus.BAD_REQUEST);
        }

        // 사업계획서 검증
        String planFileBase64 = trimToNull(request.getPlanFileBase64());
        if (planFileBase64 == null) {
            return new ResponseEntity<>("plan file required", HttpStatus.BAD_REQUEST);
        }

        // 이미 존재하는 사업자등록번호인지 검증
        CompanyRegistered existingCompany = companyRegisteredRepository.findByBusinessNumber(businessNumber);
        if (existingCompany != null
                && CompanyStatus.ACTIVE.name().equalsIgnoreCase(existingCompany.getCompanyStatus().toString())) {
            return new ResponseEntity<>("business number already registered", HttpStatus.CONFLICT);
        }

        // 설명
        String description = trimToNull(request.getDescription());

        // 사업계획서 디코딩
        byte[] planFile;

        try {
            planFile = decodePlanFile(planFileBase64);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>("invalid plan file payload", HttpStatus.BAD_REQUEST);
        }

        // Timestamp 설정
        LocalDateTime now = LocalDateTime.now();

        // 객체 조립
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

        // CompanyRegistered 조립
        CompanyRegistered companyRegistered = CompanyRegistered.builder()
                .companyName(companyName)
                .businessNumber(businessNumber)
                .sellerId(seller.getSellerId())
                .createdAt(now)
                .companyStatus(CompanyStatus.valueOf(CompanyStatus.ACTIVE.name()))
                .build();

        companyRegisteredRepository.save(companyRegistered);

        // SellerRegister 조립
        SellerRegister sellerRegister = SellerRegister.builder()
                .planFile(planFile)
                .sellerId(seller.getSellerId())
                .description(description)
                .companyName(companyName)
                .build();

        SellerRegister savedRegister = sellerRegisterRepository.save(sellerRegister);

        sellerPlanEvaluationService.evaluateAndSave(savedRegister);

        // 최초 가입 판매자 등급 부여 (임시)
        SellerGrade sellerGrade = SellerGrade.builder()
                .grade(SellerGradeEnum.valueOf(SellerGradeEnum.C.name()))
                .gradeStatus(SellerGradeStatus.valueOf(SellerGradeStatus.REVIEW.name()))
                .createdAt(now)
                .updatedAt(now)
                .expiredAt(now.plusYears(1))
                .companyId(companyRegistered.getCompanyId())
                .build();

        sellerGradeRepository.save(sellerGrade);

        clearAuthCookies(response);

        // 폰 인증 세션 삭제
        clearPhoneSession(session);

        return new ResponseEntity<>(
                "판매자 회원 가입 신청이 완료되었습니다. 관리자 승인 후에 서비스 이용이 가능합니다.",
                HttpStatus.OK
        );
    }

    // 초대받은 판매자 회원 가입
    private ResponseEntity<?> completeInvitedSellerSignup(
            CustomOAuth2User user,
            SocialSignupRequest request,
            HttpServletResponse response,
            HttpSession session,
            String storedPhone,
            String inviteToken
    ) {
        // 토큰으로 초대 생성 여부 확인
        Invitation invitation = invitationRepository.findByToken(inviteToken);
        if (invitation == null) {
            return new ResponseEntity<>("invitation not found", HttpStatus.NOT_FOUND);
        }

        // 초대 상태가 PENDING이 아니면
        if (!InvitationStatus.PENDING.name().equalsIgnoreCase(String.valueOf(invitation.getStatus()))) {
            return new ResponseEntity<>("invitation already used", HttpStatus.CONFLICT);
        }

        // 초대 만료 검증
        LocalDateTime now = LocalDateTime.now();
        if (invitation.getExpiredAt() != null && invitation.getExpiredAt().isBefore(now)) {
            invitation.setStatus(InvitationStatus.valueOf(InvitationStatus.EXPIRED.name()));
            invitation.setUpdatedAt(now);
            invitationRepository.save(invitation);
            return new ResponseEntity<>("invitation expired", HttpStatus.GONE);
        }

        // 이메일 검증
        String inviteEmail = trimToNull(invitation.getEmail());
        String signupEmail = trimToNull(user.getEmail());
        if (inviteEmail == null || signupEmail == null || !inviteEmail.equalsIgnoreCase(signupEmail)) {
            return new ResponseEntity<>("invitation email mismatch", HttpStatus.BAD_REQUEST);
        }

        // 초대한 OWNER가 존재하는지
        Seller ownerSeller = sellerRepository.findById(invitation.getSellerId()).orElse(null);
        if (ownerSeller == null) {
            return new ResponseEntity<>("invitation owner not found", HttpStatus.NOT_FOUND);
        }

        String businessNumber = trimToNull(request.getBusinessNumber());
        if (businessNumber == null) {
            return new ResponseEntity<>("business number required", HttpStatus.BAD_REQUEST);
        }

        String companyName = trimToNull(request.getCompanyName());
        if (companyName == null) {
            return new ResponseEntity<>("company name required", HttpStatus.BAD_REQUEST);
        }

        CompanyRegistered ownerCompany = companyRegisteredRepository.findBySellerId(ownerSeller.getSellerId());
        if (ownerCompany == null) {
            return new ResponseEntity<>("owner company not found", HttpStatus.NOT_FOUND);
        }

        String ownerBusinessNumber = trimToNull(ownerCompany.getBusinessNumber());
        String ownerCompanyName = trimToNull(ownerCompany.getCompanyName());
        if (!businessNumber.equals(ownerBusinessNumber)) {
            return new ResponseEntity<>("business number mismatch", HttpStatus.BAD_REQUEST);
        }

        if (!companyName.equals(ownerCompanyName)) {
            return new ResponseEntity<>("company name mismatch", HttpStatus.BAD_REQUEST);
        }

        // 객체 조립
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

        // 상태 변경(수락)
        invitation.setStatus(InvitationStatus.valueOf(InvitationStatus.ACCEPTED.name()));
        invitation.setUpdatedAt(now);
        invitationRepository.save(invitation);

        clearAuthCookies(response);

        clearPhoneSession(session);

        return new ResponseEntity<>("invited seller signup completed", HttpStatus.OK);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(expireCookie("access"));
        response.addCookie(expireCookie("refresh"));
    }

    private Cookie expireCookie(String key) {
        Cookie cookie = new Cookie(key, "");
        cookie.setMaxAge(0);
        // cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    // 폰 인증 세션 삭제
    private void clearPhoneSession(HttpSession session) {
        session.removeAttribute(SESSION_PHONE_NUMBER);
        session.removeAttribute(SESSION_PHONE_CODE);
        session.removeAttribute(SESSION_PHONE_VERIFIED);
    }

    // 사업계획서 디코딩
    private byte[] decodePlanFile(String encodedPlanFile) {
        int commaIndex = encodedPlanFile.indexOf(',');
        String payload = commaIndex >= 0 ? encodedPlanFile.substring(commaIndex + 1) : encodedPlanFile;
        return Base64.getDecoder().decode(payload);
    }

    private String trimToNull(String value) {
        // Trim 작업
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String normalizeMemberType(String memberType) {
        // Upper-case
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



