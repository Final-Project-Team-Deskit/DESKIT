package com.deskit.deskit.livechat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LiveViewerCountDTO {
    private Long broadcastId;
    private int viewers;
}
