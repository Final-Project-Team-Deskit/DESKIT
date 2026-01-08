package com.deskit.deskit.livechat.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatMessageDTO {
    private Long broadcastId;
    private String memberEmail;
    private LiveMessageType type;
    private String sender;
    private String content;
    private int vodPlayTime;
    private Long sentAt;
}
