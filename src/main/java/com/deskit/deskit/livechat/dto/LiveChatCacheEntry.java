package com.deskit.deskit.livechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatCacheEntry {
    private Long broadcastId;
    private String memberEmail;
    private LiveMessageType type;
    private String sender;
    private String content;
    private int vodPlayTime;
    private Long sentAt;
}
