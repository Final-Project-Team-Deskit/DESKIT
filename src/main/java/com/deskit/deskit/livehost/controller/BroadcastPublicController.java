package com.deskit.deskit.livehost.controller;

import com.deskit.deskit.livehost.common.exception.ApiResult;
import com.deskit.deskit.livehost.dto.request.BroadcastSearch;
import com.deskit.deskit.livehost.dto.request.OpenViduRecordingWebhook;
import com.deskit.deskit.livehost.dto.response.BroadcastProductResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastStatsResponse;
import com.deskit.deskit.livehost.service.BroadcastService;
import com.deskit.deskit.livehost.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BroadcastPublicController {

    private final BroadcastService broadcastService;
    private final SseService sseService;

    @GetMapping("/broadcasts")
    public ResponseEntity<ApiResult<Object>> getBroadcasts(
            @ModelAttribute BroadcastSearch searchCondition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getPublicBroadcasts(searchCondition, pageable)
        ));
    }

    @GetMapping("/broadcasts/{broadcastId}")
    public ResponseEntity<ApiResult<BroadcastResponse>> getBroadcastDetail(
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getPublicBroadcastDetail(broadcastId)
        ));
    }

    @PostMapping("/broadcasts/{broadcastId}/join")
    public ResponseEntity<ApiResult<String>> joinBroadcast(
            @PathVariable Long broadcastId,
            @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId
    ) {
        String token = broadcastService.joinBroadcast(broadcastId, viewerId);
        return ResponseEntity.ok(ApiResult.success(token));
    }

    @GetMapping("/broadcasts/{broadcastId}/stats")
    public ResponseEntity<ApiResult<BroadcastStatsResponse>> getBroadcastStats(
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastStats(broadcastId)
        ));
    }

    @GetMapping("/broadcasts/{broadcastId}/products")
    public ResponseEntity<ApiResult<List<BroadcastProductResponse>>> getProducts(@PathVariable Long broadcastId) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastProducts(broadcastId)
        ));
    }

    @GetMapping("/broadcasts/{broadcastId}/chat-permission")
    public ResponseEntity<ApiResult<Boolean>> getChatPermission(
            @PathVariable Long broadcastId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.canChat(broadcastId, memberId)
        ));
    }

    @GetMapping(value = "/broadcasts/{broadcastId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable Long broadcastId,
            @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId,
            @RequestParam(value = "viewerId", required = false) String viewerIdParam
    ) {
        String userId = (viewerId != null) ? viewerId : (viewerIdParam != null ? viewerIdParam : "anonymous");
        return sseService.subscribe(broadcastId, userId);
    }

    @PostMapping("/webhook/openvidu")
    public ResponseEntity<Void> handleWebhook(@RequestBody OpenViduRecordingWebhook payload) {
        if ("recordingStatusChanged".equals(payload.getEvent()) && "ready".equals(payload.getStatus())) {
            broadcastService.processVod(payload);
        }
        return ResponseEntity.ok().build();
    }
}
