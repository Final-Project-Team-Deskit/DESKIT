package com.deskit.deskit.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1) 요청 권한(Authorization) 규칙 정의
        http.authorizeHttpRequests(auth -> auth
                        // ✅ 비회원(익명)도 볼 수 있는 공개 조회 API
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/setups", "/api/setups/**").permitAll()

                        // ✅ 스프링 시큐리티 로그인/에러 관련 경로는 공개
                        .requestMatchers("/login**", "/oauth2/**", "/error").permitAll()

                        // ✅ 그 외 모든 요청은 인증 필요(장바구니/주문/구매 등)
                        .anyRequest().authenticated()
                )
                // 2) OAuth2 로그인 사용(구글 등)
                .oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}