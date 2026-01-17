package com.deskit.deskit.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 모든 경로(/**)에 대해 정적 리소스 위치 매핑
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 1. 실제 파일이 존재하면 그 파일 반환 (js, css, 이미지 등)
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 2. API, WebSocket 등 백엔드 전용 경로는 SPA 리다이렉트에서 제외 (404 반환하여 다른 핸들러로 넘김)
                        if (resourcePath.startsWith("api") ||
                                resourcePath.startsWith("ws") ||
                                resourcePath.startsWith("openvidu") ||
                                resourcePath.startsWith("oauth") ||
                                resourcePath.startsWith("login")) {
                            return null;
                        }

                        // 3. 그 외 경로는 모두 index.html 반환 (SPA 라우팅)
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }

    // [추가] 루트 경로("/") 접속 시 index.html로 이동
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}