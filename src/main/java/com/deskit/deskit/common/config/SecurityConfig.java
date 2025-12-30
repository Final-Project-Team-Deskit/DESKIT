package com.deskit.deskit.common.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            // 프론트(localhost:5174) → 백엔드 API 호출을 허용하기 위해 CORS 설정 사용
            .cors(Customizer.withDefaults())

            // /api/** 요청은 브라우저/프론트 호출이 많아서 CSRF 예외 처리 (API는 토큰/세션 정책에 맞게 별도 설계 가능)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

            // 접근 제어 규칙
            .authorizeHttpRequests(auth -> auth
                    // 프리플라이트(OPTIONS) 요청은 무조건 허용 (CORS 통과 목적)
                    .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()

                    // 상품/셋업 조회 API는 로그인 없이도 접근 가능하도록 허용
                    .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/setups/**").permitAll()

                    // 로그인/소셜로그인 관련 엔드포인트 및 에러 페이지는 허용
                    .requestMatchers("/login**", "/oauth2/**", "/error").permitAll()

                    // 그 외 모든 요청은 인증 필요
                    .anyRequest().authenticated())

            // API 호출에서 인증/인가 실패 시 브라우저 리다이렉트 대신 상태코드로 응답하도록 처리
            .exceptionHandling(exceptions -> exceptions
                    // /api/** 에서 인증 안 됐으면 401 반환
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            new AntPathRequestMatcher("/api/**"))
                    // /api/** 에서 권한 부족이면 403 반환
                    .defaultAccessDeniedHandlerFor(
                            accessDeniedHandler(),
                            new AntPathRequestMatcher("/api/**")))

            // OAuth2 로그인 사용 (구글 등)
            .oauth2Login(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 프론트 개발 서버 origin 허용
    configuration.setAllowedOrigins(List.of("http://localhost:5174"));

    // 허용할 HTTP 메서드
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // 허용할 헤더 (개발 편의상 전체 허용)
    configuration.setAllowedHeaders(List.of("*"));

    // 쿠키/세션 기반 인증을 위해 credentials 허용
    configuration.setAllowCredentials(true);

    // /api/** 경로에만 위 CORS 정책 적용
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  // API 권한 부족(인가 실패) 시 403 응답
  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) ->
            response.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
  }
}