package com.deskit.deskit.livehost.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public String getRealtimeViewKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":active_uv";
    }

    public String getSessionCountKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":session_counts";
    }

    public String getTotalUvKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":total_uv";
    }

    public String getLikeUsersKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":like_users";
    }

    public String getSanctionKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":sanctions";
    }

    public String getReportUsersKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":report_users";
    }

    public String getReportCountKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":reports";
    }

    public String getViewHistoryBufferKey(String type) {
        return "view_history:buffer:" + type;
    }

    public String getMaxViewersKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":max_viewers";
    }

    public String getMaxViewersTimeKey(Long broadcastId) {
        return "broadcast:" + broadcastId + ":max_viewers_time";
    }

    public String getMediaConfigKey(Long broadcastId, Long sellerId) {
        return "broadcast:" + broadcastId + ":media:" + sellerId;
    }

    public String getScheduleNoticeKey(Long broadcastId, String type) {
        return "broadcast:" + broadcastId + ":notice:" + type;
    }

    public Boolean acquireLock(String key, long timeoutMillis) {
        return redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofMillis(timeoutMillis));
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(result);
    }

    public void enterLiveRoom(Long broadcastId, String uuid) {
        String sessionKey = getSessionCountKey(broadcastId);
        String activeKey = getRealtimeViewKey(broadcastId);
        String totalKey = getTotalUvKey(broadcastId);

        Long count = redisTemplate.opsForHash().increment(sessionKey, uuid, 1);

        if (count != null && count == 1) {
            redisTemplate.opsForSet().add(activeKey, uuid);
            updatePeakViewers(broadcastId);
        }

        redisTemplate.opsForSet().add(totalKey, uuid);

        expireKey(sessionKey);
        expireKey(activeKey);
        expireKey(totalKey);
    }

    public void exitLiveRoom(Long broadcastId, String uuid) {
        String sessionKey = getSessionCountKey(broadcastId);
        String activeKey = getRealtimeViewKey(broadcastId);

        Long count = redisTemplate.opsForHash().increment(sessionKey, uuid, -1);

        if (count != null && count <= 0) {
            redisTemplate.opsForHash().delete(sessionKey, uuid);
            redisTemplate.opsForSet().remove(activeKey, uuid);
        }
    }

    public int getRealtimeViewerCount(Long broadcastId) {
        Long size = redisTemplate.opsForSet().size(getRealtimeViewKey(broadcastId));
        return size != null ? size.intValue() : 0;
    }

    public int getTotalUniqueViewerCount(Long broadcastId) {
        Long size = redisTemplate.opsForSet().size(getTotalUvKey(broadcastId));
        return size != null ? size.intValue() : 0;
    }

    public void bufferViewHistory(String type, Long broadcastId, String viewerId) {
        String value = broadcastId + ":" + viewerId + ":" + System.currentTimeMillis();
        redisTemplate.opsForList().rightPush(getViewHistoryBufferKey(type), value);
    }

    public List<String> popViewHistoryBuffer(String type, int count) {
        String key = getViewHistoryBufferKey(type);
        List<Object> objects = redisTemplate.opsForList().range(key, 0, count - 1);
        if (objects == null || objects.isEmpty()) {
            return List.of();
        }

        redisTemplate.opsForList().trim(key, count, -1);

        return objects.stream().map(Object::toString).collect(Collectors.toList());
    }

    public boolean toggleLike(Long broadcastId, Long memberId) {
        String key = getLikeUsersKey(broadcastId);

        Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(memberId));

        if (Boolean.TRUE.equals(isMember)) {
            redisTemplate.opsForSet().remove(key, String.valueOf(memberId));
            return false;
        }

        redisTemplate.opsForSet().add(key, String.valueOf(memberId));
        expireKey(key);
        return true;
    }

    public int getLikeCount(Long broadcastId) {
        Long size = redisTemplate.opsForSet().size(getLikeUsersKey(broadcastId));
        return size != null ? size.intValue() : 0;
    }

    public boolean reportBroadcast(Long broadcastId, Long memberId) {
        String userKey = getReportUsersKey(broadcastId);
        String countKey = getReportCountKey(broadcastId);

        Long added = redisTemplate.opsForSet().add(userKey, String.valueOf(memberId));

        if (added != null && added == 1) {
            redisTemplate.opsForValue().increment(countKey);
            redisTemplate.expire(userKey, Duration.ofDays(1));
            return true;
        }

        return false;
    }

    public int getReportCount(Long broadcastId) {
        return getInt(getReportCountKey(broadcastId));
    }

    public void saveMediaConfig(Long broadcastId, Long sellerId, String cameraId, String microphoneId, boolean cameraOn, boolean microphoneOn, int volume) {
        String key = getMediaConfigKey(broadcastId, sellerId);
        redisTemplate.opsForHash().put(key, "cameraId", cameraId);
        redisTemplate.opsForHash().put(key, "microphoneId", microphoneId);
        redisTemplate.opsForHash().put(key, "cameraOn", String.valueOf(cameraOn));
        redisTemplate.opsForHash().put(key, "microphoneOn", String.valueOf(microphoneOn));
        redisTemplate.opsForHash().put(key, "volume", String.valueOf(volume));
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    public List<Object> getMediaConfig(Long broadcastId, Long sellerId) {
        String key = getMediaConfigKey(broadcastId, sellerId);
        return redisTemplate.opsForHash().multiGet(key, List.of("cameraId", "microphoneId", "cameraOn", "microphoneOn", "volume"));
    }

    public int getMaxViewers(Long broadcastId) {
        return getInt(getMaxViewersKey(broadcastId));
    }

    public LocalDateTime getMaxViewersTime(Long broadcastId) {
        Object val = redisTemplate.opsForValue().get(getMaxViewersTimeKey(broadcastId));
        return val != null ? LocalDateTime.parse(val.toString()) : null;
    }

    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    public Integer getStatOrZero(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void publish(String channelTopic, Object message) {
        redisTemplate.convertAndSend(channelTopic, message);
    }

    public void deleteBroadcastKeys(Long broadcastId) {
        redisTemplate.delete(getRealtimeViewKey(broadcastId));
        redisTemplate.delete(getSessionCountKey(broadcastId));
        redisTemplate.delete(getTotalUvKey(broadcastId));
        redisTemplate.delete(getLikeUsersKey(broadcastId));
        redisTemplate.delete(getSanctionKey(broadcastId));
        redisTemplate.delete(getReportUsersKey(broadcastId));
        redisTemplate.delete(getReportCountKey(broadcastId));
        redisTemplate.delete(getMaxViewersKey(broadcastId));
        redisTemplate.delete(getMaxViewersTimeKey(broadcastId));
    }

    private void expireKey(String key) {
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    private void updatePeakViewers(Long broadcastId) {
        int current = getRealtimeViewerCount(broadcastId);
        String maxKey = getMaxViewersKey(broadcastId);

        Integer max = getInt(maxKey);

        if (current > max) {
            redisTemplate.opsForValue().set(maxKey, String.valueOf(current));
            redisTemplate.opsForValue().set(getMaxViewersTimeKey(broadcastId), LocalDateTime.now().toString());
        }
    }

    private int getInt(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
