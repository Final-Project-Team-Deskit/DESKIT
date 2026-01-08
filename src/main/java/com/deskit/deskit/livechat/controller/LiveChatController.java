package com.deskit.deskit.livechat.controller;

import com.deskit.deskit.livechat.dto.LiveChatMessageDTO;
import com.deskit.deskit.livechat.service.LiveChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LiveChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final LiveChatService chatService;

    @MessageMapping("/chat/message")
    public void handleMessage(LiveChatMessageDTO message, Principal principal) {
        String filtered = chatService.filterContent(message.getContent());
        message.setContent(filtered);

        if (message.getSentAt() == null) {
            message.setSentAt(System.currentTimeMillis());
        }
        String principalName = principal != null ? principal.getName() : "anonymous";
        log.debug("livechat.in broadcastId={} sender={} type={} sentAt={} principal={} content={}",
                message.getBroadcastId(),
                message.getSender(),
                message.getType(),
                message.getSentAt(),
                principalName,
                message.getContent());

        chatService.saveMessageAsync(message);
        chatService.cacheRecentMessage(message);

        messagingTemplate.convertAndSend("/sub/chat/" + message.getBroadcastId(), message);
    }

    @GetMapping("/livechats/{broadcastId}/recent")
    public List<LiveChatMessageDTO> getRecentTalks(
            @PathVariable Long broadcastId,
            @RequestParam(name = "seconds", required = false) Long seconds
    ) {
        log.debug("livechat.recent.request broadcastId={} seconds={}", broadcastId, seconds);
        List<LiveChatMessageDTO> result = chatService.getRecentTalks(broadcastId, seconds);
        log.debug("livechat.recent.response broadcastId={} count={}", broadcastId, result.size());
        return result;
    }
}
