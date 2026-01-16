package com.deskit.deskit.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/{path:[^\\.]*}"})
    public String forwardRoot(HttpServletRequest request) {
        return forwardIfSpa(request);
    }

    @RequestMapping(value = "/**/{path:[^\\.]*}")
    public String forwardDeep(HttpServletRequest request) {
        return forwardIfSpa(request);
    }

    private String forwardIfSpa(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api")
                || uri.startsWith("/ws")
                || uri.startsWith("/openvidu")
                || uri.startsWith("/oauth")
                || uri.startsWith("/login")
                || uri.startsWith("/oauth2")) {
            return null; // SPA forward 대상 아님
        }

        return "forward:/index.html";
    }
}
