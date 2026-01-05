package com.deskit.deskit.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // 비동기 기능을 활성화 -> @Async 붙이면 됨
public class AsyncConfig {
}
