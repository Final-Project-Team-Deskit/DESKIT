package com.deskit.deskit.livechat.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatMessageDTO {
    private Long broadcastId;
    private Long memberId;
    private LiveMessageType type;
    private String sender;
    private String content;
    private int vodPlayTime;
}
