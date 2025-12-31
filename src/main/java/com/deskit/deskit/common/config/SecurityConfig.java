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
            // 프론트(5174)에서 /api 호출할 때 CORS 허용
            .cors(Customizer.withDefaults())

            // API는 CSRF 토큰 없이도 호출 가능하도록 예외 처리
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

            // 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                    // 프리플라이트(OPTIONS)는 항상 허용
                    .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                    // 상품/셋업 조회 GET은 로그인 없이 허용
                    .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/setups/**", "/api/home/**")
                    .permitAll()
                    // 로그인/OAuth/에러 페이지는 허용
                    .requestMatchers("/login**", "/oauth2/**", "/error").permitAll()
                    // 나머지는 인증 필요
                    .anyRequest().authenticated())

            // API 요청에서 인증/권한 오류를 "리다이렉트" 대신 HTTP 상태코드로 내려주기
            .exceptionHandling(exceptions -> exceptions
                    // 인증 안 된 상태로 /api/** 접근하면 401
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            new AntPathRequestMatcher("/api/**"))
                    // 권한 부족으로 /api/** 접근하면 403
                    .defaultAccessDeniedHandlerFor(
                            accessDeniedHandler(),
                            new AntPathRequestMatcher("/api/**")))

            // OAuth2 로그인 사용
            .oauth2Login(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // dev 프론트 origin 허용
    configuration.setAllowedOrigins(List.of("http://localhost:5174"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    // 세션 쿠키(예: JSESSIONID) 사용 가능
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) ->
            response.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
  }
}
