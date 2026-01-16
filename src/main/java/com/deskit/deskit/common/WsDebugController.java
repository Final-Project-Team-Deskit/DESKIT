package com.deskit.deskit.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WsDebugController {

    @GetMapping("/ws/info")
    public Map<String, Object> debug() {
        return Map.of("debug", true);
    }
}

