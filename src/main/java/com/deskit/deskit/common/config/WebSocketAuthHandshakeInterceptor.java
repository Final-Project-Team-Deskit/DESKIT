package com.deskit.deskit.common.config;

import com.deskit.deskit.account.jwt.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthHandshakeInterceptor.class);

    private final JWTUtil jwtUtil;

    public WebSocketAuthHandshakeInterceptor(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        if (httpRequest.getCookies() == null || httpRequest.getCookies().length == 0) {
            log.debug("ws.handshake no cookies");
        } else {
            log.debug("ws.handshake cookies={}", cookieNames(httpRequest.getCookies()));
        }
        String token = resolveToken(httpRequest);
        if (token == null || token.isBlank()) {
            log.debug("ws.handshake no token");
            return true;
        }

        try {
            if (jwtUtil.isExpired(token)) {
                return true;
            }
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            return true;
        }

        String category = jwtUtil.getCategory(token);
        if (!"access".equals(category)) {
            log.debug("ws.handshake invalid category={}", category);
            return true;
        }

        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);
        attributes.put("principal", new WebSocketPrincipal(username));
        log.debug("ws.handshake principal set username={} role={}", username, role);

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            @Nullable Exception exception
    ) {
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        String legacy = request.getHeader("access");
        if (legacy != null && !legacy.isBlank()) {
            return legacy;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("access".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String cookieNames(Cookie[] cookies) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cookies.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(cookies[i].getName());
        }
        return builder.toString();
    }
}
