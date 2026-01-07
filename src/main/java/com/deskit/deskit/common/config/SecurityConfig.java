package com.deskit.deskit.common.config;

import com.deskit.deskit.account.jwt.CustomLogoutFilter;
import com.deskit.deskit.account.jwt.JWTFilter;
import com.deskit.deskit.account.jwt.JWTUtil;
import com.deskit.deskit.account.oauth.CustomOAuth2FailureHandler;
import com.deskit.deskit.account.oauth.CustomSuccessHandler;
import com.deskit.deskit.account.repository.RefreshRepository;
import com.deskit.deskit.account.service.CustomOAuth2UserService;
import com.deskit.deskit.admin.security.AdminSecondFactorFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          CustomSuccessHandler customSuccessHandler,
                          CustomOAuth2FailureHandler customOAuth2FailureHandler,
                          JWTUtil jwtUtil,
                          RefreshRepository refreshRepository) {

        this.customOAuth2UserService = customOAuth2UserService;
        this.customSuccessHandler = customSuccessHandler;
        this.customOAuth2FailureHandler = customOAuth2FailureHandler;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {

                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:5173"));
                        configuration.setAllowedMethods(Collections.singletonList("*"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        configuration.setMaxAge(3600L);

                        configuration.setExposedHeaders(
                                java.util.List.of("Set-Cookie", "Authorization", "access")
                        );

                        return configuration;
                    }
                }));

        //csrf disable
        http
                .csrf((auth) -> auth.disable());

        //From 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        //HTTP Basic 인증 방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        http
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write("{\"message\":\"인증이 필요합니다\"}");
                                },
                                new AntPathRequestMatcher("/api/**") // API만
                        )
                );

        //JWTFilter 추가
        http
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new AdminSecondFactorFilter(), JWTFilter.class);

        http
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), LogoutFilter.class);

        //oauth2
        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                        .successHandler(customSuccessHandler)
                        .failureHandler(customOAuth2FailureHandler)
                );

        //경로별 인가 작업
		http
				.authorizeHttpRequests((auth) -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET,
								"/api/products/**",
								"/api/setups/**",
								"/api/setup/**",
								"/api/products",
								"/api/setups",
								"/api/home/**",
								"/products/**",
								"/setups/**"
						).permitAll()
						.requestMatchers(
								"/",
                "/chat",
                "/chat/**",
                "/reissue",
                "/api/home/**",
                "/api/admin/auth/**",
                "/api/invitations/validate",
                "/oauth/**",
								"/login",
								"/login/**",
								"/login/oauth2/**"
						).permitAll()
						.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers("/api/quit").hasAnyAuthority(
								"ROLE_MEMBER",
								"ROLE_SELLER_OWNER",
								"ROLE_SELLER_MANAGER"
						)
						.requestMatchers("/my").hasAnyAuthority(
								"ROLE_MEMBER",
								"ROLE_SELLER",
								"ROLE_SELLER_OWNER",
								"ROLE_SELLER_MANAGER",
								"ROLE_ADMIN"
						)
						.anyRequest().authenticated());

        //세션 설정 : STATELESS -> IF_REQUIRED
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}
