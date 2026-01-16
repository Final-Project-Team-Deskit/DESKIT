package com.deskit.deskit.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/**/{path:[^\\.]*}"})
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // ✅ API/WS/OpenVidu/OAuth는 SPA forward 대상 아님
        if (uri.startsWith("/api/")
                || uri.equals("/api")
                || uri.startsWith("/ws/")
                || uri.equals("/ws")
                || uri.startsWith("/openvidu/")
                || uri.equals("/openvidu")
                || uri.startsWith("/oauth/")
                || uri.equals("/oauth")
                || uri.startsWith("/login")      // 필요시 포함 (로그인도 백엔드가 처리)
                || uri.startsWith("/oauth2/")    // 필요시 포함
        ) {
            return null; // forward 하지 말고, 원래 핸들러(컨트롤러/웹소켓)가 처리하게 둠
        }

        return "forward:/index.html";
    }
}
