package com.deskit.deskit.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = "/{path:^(?!api|ws|openvidu|oauth).*}/**")
    public String forward() {
        return "forward:/index.html";
    }
}
