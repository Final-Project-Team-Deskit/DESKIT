package com.deskit.deskit.livechat.service;

import com.deskit.deskit.livechat.dto.LiveChatMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String topic, LiveChatMessage message) {
        redisTemplate.convertAndSend(topic, message);
    }
}
