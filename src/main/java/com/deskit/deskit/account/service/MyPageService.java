package com.deskit.deskit.account.service;

import com.deskit.deskit.account.controller.MyPageController;
import com.deskit.deskit.account.dto.MyPageResponse;
import com.deskit.deskit.account.entity.Member;
import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.oauth.CustomOAuth2User;
import com.deskit.deskit.account.repository.MemberRepository;
import com.deskit.deskit.account.repository.SellerRepository;
import com.deskit.deskit.admin.entity.Admin;
import com.deskit.deskit.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyPageService {

	private final MemberRepository memberRepository;
	private final SellerRepository sellerRepository;
	private final AdminRepository adminRepository;

	public MyPageResponse buildMyPageResponse(CustomOAuth2User user, String role) {
		String normalizedRole = role == null ? "" : role.trim();
		String loginId = safe(user.getUsername());
		String name = safe(user.getName());
		String email = safe(user.getEmail());

		if (email.isEmpty() && !loginId.isEmpty()) {
			email = loginId;
		}

		if (name.isEmpty() && !loginId.isEmpty()) {
			name = resolveName(normalizedRole, loginId);
		}

		return new MyPageResponse(
				name,
				email,
				normalizedRole,
				resolveMemberCategory(normalizedRole),
				resolveSellerRole(normalizedRole)
		);
	}

	private String resolveName(String role, String loginId) {
		return switch (role) {
			case "ROLE_ADMIN" -> {
				Admin admin = adminRepository.findByLoginId(loginId);
				yield admin == null ? "" : safe(admin.getName());
			}
			case "ROLE_MEMBER" -> {
				Member member = memberRepository.findByLoginId(loginId);
				yield member == null ? "" : safe(member.getName());
			}
			default -> {
				if (role.startsWith("ROLE_SELLER")) {
					Seller seller = sellerRepository.findByLoginId(loginId);
					yield seller == null ? "" : safe(seller.getName());
				}
				yield "";
			}
		};
	}

	private String resolveMemberCategory(String role) {
		if (role == null || role.isBlank()) {
			return "";
		}
		return switch (role) {
			case "ROLE_ADMIN" -> "관리자";
			case "ROLE_MEMBER" -> "일반회원";
			case "ROLE_SELLER", "ROLE_SELLER_OWNER", "ROLE_SELLER_MANAGER" -> "판매자";
			default -> "";
		};
	}

	private String resolveSellerRole(String role) {
		if (role == null || role.isBlank()) {
			return "";
		}
		return switch (role) {
			case "ROLE_SELLER_OWNER" -> "오너";
			case "ROLE_SELLER_MANAGER" -> "매니저";
			default -> "";
		};
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
