package com.deskit.deskit.account.controller;

import com.deskit.deskit.account.entity.Member;
import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.enums.MemberStatus;
import com.deskit.deskit.account.enums.SellerRole;
import com.deskit.deskit.account.enums.SellerStatus;
import com.deskit.deskit.account.oauth.CustomOAuth2User;
import com.deskit.deskit.account.repository.MemberRepository;
import com.deskit.deskit.account.repository.RefreshRepository;
import com.deskit.deskit.account.repository.SellerRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QuitController {

	private final MemberRepository memberRepository;
	private final SellerRepository sellerRepository;
	private final RefreshRepository refreshRepository;

	@PostMapping("/api/quit")
	@Transactional
	public ResponseEntity<?> quit(
			@AuthenticationPrincipal CustomOAuth2User user,
			Authentication authentication,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		if (user == null || authentication == null) {
			return new ResponseEntity<>(Map.of("message", "unauthorized"), HttpStatus.UNAUTHORIZED);
		}

		String loginId = user.getUsername();
		String role = authentication.getAuthorities().iterator().next().getAuthority();

		if ("ROLE_MEMBER".equals(role)) {
			return withdrawMember(loginId, request, response);
		}

		if ("ROLE_SELLER_OWNER".equals(role)) {
			return new ResponseEntity<>(Map.of("message", "OWNER 권한은 탈퇴할 수 없습니다."), HttpStatus.FORBIDDEN);
		}

		if ("ROLE_SELLER_MANAGER".equals(role)) {
			return withdrawSeller(loginId, request, response);
		}

		return new ResponseEntity<>(Map.of("message", "unsupported role"), HttpStatus.FORBIDDEN);
	}

	private ResponseEntity<?> withdrawMember(
			String loginId,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		Member member = memberRepository.findByLoginId(loginId);
		if (member == null) {
			return new ResponseEntity<>(Map.of("message", "member not found"), HttpStatus.NOT_FOUND);
		}
		if (member.getStatus() == MemberStatus.INACTIVE) {
			return new ResponseEntity<>(Map.of("message", "이미 탈퇴된 회원입니다."), HttpStatus.CONFLICT);
		}

		member.setStatus(MemberStatus.INACTIVE);
		memberRepository.save(member);

		clearTokens(request, response);

		return new ResponseEntity<>(Map.of("message", "회원 탈퇴가 완료되었습니다."), HttpStatus.OK);
	}

	private ResponseEntity<?> withdrawSeller(
			String loginId,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		Seller seller = sellerRepository.findByLoginId(loginId);
		if (seller == null) {
			return new ResponseEntity<>(Map.of("message", "seller not found"), HttpStatus.NOT_FOUND);
		}
		if (seller.getRole() == SellerRole.ROLE_SELLER_OWNER) {
			return new ResponseEntity<>(Map.of("message", "OWNER 권한은 탈퇴할 수 없습니다."), HttpStatus.FORBIDDEN);
		}
		if (seller.getStatus() == SellerStatus.INACTIVE) {
			return new ResponseEntity<>(Map.of("message", "이미 탈퇴된 판매자입니다."), HttpStatus.CONFLICT);
		}

		seller.setStatus(SellerStatus.INACTIVE);
		sellerRepository.save(seller);

		clearTokens(request, response);

		return new ResponseEntity<>(Map.of("message", "회원 탈퇴가 완료되었습니다."), HttpStatus.OK);
	}

	private void clearTokens(HttpServletRequest request, HttpServletResponse response) {
		String refresh = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refresh".equals(cookie.getName())) {
					refresh = cookie.getValue();
					break;
				}
			}
		}

		if (refresh != null && !refresh.isBlank()) {
			refreshRepository.deleteByRefresh(refresh);
		}

		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		response.setHeader("access", "");
		response.addCookie(expireCookie("access"));
		response.addCookie(expireCookie("refresh"));
	}

	private Cookie expireCookie(String key) {
		Cookie cookie = new Cookie(key, null);
		cookie.setMaxAge(0);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		return cookie;
	}
}
