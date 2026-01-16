package com.deskit.deskit.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = "/**/{path:[^\\.]*}")
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api")
                || uri.startsWith("/ws")
                || uri.startsWith("/openvidu")
                || uri.startsWith("/oauth")) {
            return null; // forward하지 않음
        }

        return "forward:/index.html";
    }
}
