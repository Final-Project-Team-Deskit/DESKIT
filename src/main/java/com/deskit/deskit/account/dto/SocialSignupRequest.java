package com.deskit.deskit.account.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialSignupRequest {

    // 판매자 또는 일반회원
    private String memberType;

    // 전화번호
    private String phoneNumber;

    // 일반회원 MBTI
    private String mbti;

    // 일반회원 직업 카테고리
    private String jobCategory;

    // 판매자 사업자등록번호
    private String businessNumber;

    // 판매자 사업자명(상호명)
    private String companyName;

    // 판매자 설명
    private String description;

    // 사업계획서(Base-64 인코딩)
    private String planFileBase64;

    // 초대 토큰
    private String inviteToken;

    // 약관 동의 체크
    private boolean isAgreed;
}
