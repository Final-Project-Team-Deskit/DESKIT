package com.deskit.deskit.livechat.service;

import com.deskit.deskit.livechat.dto.LiveChatCacheEntry;
import com.deskit.deskit.livechat.dto.LiveChatMessageDTO;
import com.deskit.deskit.livechat.dto.LiveMessageType;
import com.deskit.deskit.livechat.entity.ForbiddenWord;
import com.deskit.deskit.livechat.entity.LiveChat;
import com.deskit.deskit.livechat.repository.ForbiddenWordRepository;
import com.deskit.deskit.livechat.repository.LiveChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiveChatService {
    private static final long DEFAULT_RECENT_WINDOW_SECONDS = 60L;
    private static final String RECENT_CHAT_KEY_PREFIX = "livechat:recent:";

    private final LiveChatRepository liveChatRepository;
    private final ForbiddenWordRepository forbiddenWordRepository;
    private final ObjectMapper objectMapper;
    @Qualifier("chatRedisTemplate")
    private final RedisTemplate<String, Object> chatRedisTemplate;
    private List<ForbiddenWord> cachedWords;

    @PostConstruct
    public void init() {
        this.cachedWords = forbiddenWordRepository.findAll();
    }

    public String filterContent(String content) {
        if (content == null || cachedWords == null) {
            return content;
        }
        for (ForbiddenWord fw : cachedWords) {
            if (content.contains(fw.getWord())) {
                content = content.replace(fw.getWord(), "***");
            }
        }
        return content;
    }

    @Async("chatSaveExecutor")
    public void saveMessageAsync(LiveChatMessageDTO dto) {
        LiveChat entity = LiveChat.builder()
                .broadcastId(dto.getBroadcastId())
                .memberEmail(dto.getMemberEmail())
                .msgType(dto.getType())
                .content(dto.getContent())
                .sendNick(dto.getSender())
                .isWorld(false)
                .isHidden(false)
                .vodPlayTime(dto.getVodPlayTime())
                .build();

        liveChatRepository.save(entity);
        log.debug("livechat.db.saved broadcastId={} messageId={}",
                dto.getBroadcastId(),
                entity.getMessageId());
    }

    public void cacheRecentMessage(LiveChatMessageDTO dto) {
        if (dto == null || dto.getBroadcastId() == null) {
            return;
        }
        if (dto.getType() != LiveMessageType.TALK) {
            return;
        }
        long now = System.currentTimeMillis();
        if (dto.getSentAt() == null) {
            dto.setSentAt(now);
        }
        LiveChatCacheEntry entry = LiveChatCacheEntry.builder()
                .broadcastId(dto.getBroadcastId())
                .memberEmail(dto.getMemberEmail())
                .type(dto.getType())
                .sender(dto.getSender())
                .content(dto.getContent())
                .vodPlayTime(dto.getVodPlayTime())
                .sentAt(dto.getSentAt())
                .build();

        String key = recentChatKey(dto.getBroadcastId());
        chatRedisTemplate.opsForZSet().add(key, entry, dto.getSentAt());
        long cutoff = now - (DEFAULT_RECENT_WINDOW_SECONDS * 1000L);
        chatRedisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
        log.debug("livechat.cache.saved broadcastId={} sentAt={}", dto.getBroadcastId(), dto.getSentAt());
    }

    public List<LiveChatMessageDTO> getRecentTalks(Long broadcastId, Long seconds) {
        if (broadcastId == null) {
            return Collections.emptyList();
        }
        long windowSeconds = seconds == null
                ? DEFAULT_RECENT_WINDOW_SECONDS
                : Math.min(DEFAULT_RECENT_WINDOW_SECONDS, Math.max(1L, seconds));
        long now = System.currentTimeMillis();
        long cutoff = now - (windowSeconds * 1000L);

        String key = recentChatKey(broadcastId);
        Set<Object> raw = chatRedisTemplate.opsForZSet().rangeByScore(key, cutoff, now);
        if (raw == null || raw.isEmpty()) {
            log.debug("livechat.cache.miss broadcastId={}", broadcastId);
            return Collections.emptyList();
        }

        List<LiveChatMessageDTO> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            LiveChatCacheEntry entry = toCacheEntry(item);
            if (entry == null || entry.getType() != LiveMessageType.TALK) {
                continue;
            }
            result.add(LiveChatMessageDTO.builder()
                    .broadcastId(entry.getBroadcastId())
                    .memberEmail(entry.getMemberEmail())
                    .type(entry.getType())
                    .sender(entry.getSender())
                    .content(entry.getContent())
                    .vodPlayTime(entry.getVodPlayTime())
                    .sentAt(entry.getSentAt())
                    .build());
        }
        log.debug("livechat.cache.hit broadcastId={} count={}", broadcastId, result.size());
        return result;
    }

    private LiveChatCacheEntry toCacheEntry(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof LiveChatCacheEntry) {
            return (LiveChatCacheEntry) item;
        }
        try {
            return objectMapper.convertValue(item, LiveChatCacheEntry.class);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String recentChatKey(Long broadcastId) {
        return RECENT_CHAT_KEY_PREFIX + broadcastId;
    }
}
