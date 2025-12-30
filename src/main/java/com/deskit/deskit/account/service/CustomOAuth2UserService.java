package com.deskit.deskit.account.service;

import com.deskit.deskit.account.dto.UserDTO;
import com.deskit.deskit.account.entity.Member;
import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.oauth.*;
import com.deskit.deskit.account.repository.MemberRepository;
import com.deskit.deskit.account.repository.SellerRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;

    public CustomOAuth2UserService(MemberRepository memberRepository, SellerRepository sellerRepository) {

        this.memberRepository = memberRepository;
        this.sellerRepository = sellerRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println(oAuth2User);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;

        switch (registrationId) {
            case "google" -> oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());

            case "naver" -> oAuth2Response = new NaverResponse(oAuth2User.getAttributes());

            case "kakao" -> oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());

            default -> {
                return null;
            }
        }

        String username = oAuth2Response.getProvider()+" "+oAuth2Response.getProviderId();
        Member existMember = memberRepository.findByLoginId(oAuth2Response.getEmail());
        Seller existSeller = sellerRepository.findByLoginId(oAuth2Response.getEmail());

        // Member에도 없고 Seller에도 없는 경우 -> 신규 등록, role 임시 부여(GUEST)
        if (existMember == null && existSeller == null) {

            // Build user info for a pending signup user.
            UserDTO userDTO = UserDTO.builder()
                    .username(username)
                    .name(oAuth2Response.getName())
                    .email(oAuth2Response.getEmail())
                    .role("ROLE_GUEST")
                    .newUser(true)
                    .profileUrl(oAuth2Response.getProfileUrl() == null ? "" : oAuth2Response.getProfileUrl())
                    .build();

            return new CustomOAuth2User(userDTO);
        }

        // Member에 존재할 경우 -> Member 로그인
        else if (existMember != null) {

            existMember.setLoginId(oAuth2Response.getEmail());
            existMember.setName(oAuth2Response.getName());

            memberRepository.save(existMember);

            UserDTO userDTO = UserDTO.builder()
                    .username(existMember.getLoginId())
                    .name(oAuth2Response.getName())
                    .email(oAuth2Response.getEmail())
                    .role(existMember.getRole())
                    .newUser(false)
                    .profileUrl(oAuth2Response.getProfileUrl() == null ? "" : oAuth2Response.getProfileUrl())
                    .build();

            return new CustomOAuth2User(userDTO);
        }

        // Seller에 존재할 경우 -> Seller 로그인
        else {
            existSeller.setLoginId(oAuth2Response.getEmail());
            existSeller.setName(oAuth2Response.getName());

            sellerRepository.save(existSeller);

            UserDTO userDTO = UserDTO.builder()
                    .username(existSeller.getLoginId())
                    .name(existSeller.getName())
                    .email(oAuth2Response.getEmail())
                    .role(existSeller.getRole().toString())
                    .newUser(false)
                    .profileUrl(oAuth2Response.getProfileUrl() == null ? "" : oAuth2Response.getProfileUrl())
                    .build();

            return new CustomOAuth2User(userDTO);
        }
    }
}
