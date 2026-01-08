package com.deskit.deskit.livehost.controller.member;

import com.deskit.deskit.account.entity.Member;
import com.deskit.deskit.livehost.common.exception.ApiResult;
import com.deskit.deskit.livehost.common.utils.LiveAuthUtils;
import com.deskit.deskit.livehost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/broadcasts")
@RequiredArgsConstructor
public class BroadcastMemberController {

    private final BroadcastService broadcastService;
    private final LiveAuthUtils liveAuthUtils;

    @PostMapping("/{broadcastId}/report")
    public ResponseEntity<ApiResult<Void>> reportBroadcast(
            @PathVariable Long broadcastId
    ) {
        Member member = liveAuthUtils.getCurrentMember();
        broadcastService.reportBroadcast(broadcastId, member.getMemberId());
        return ResponseEntity.ok(ApiResult.success(null));
    }

    @PostMapping("/{broadcastId}/like")
    public ResponseEntity<ApiResult<Void>> likeBroadcast(
            @PathVariable Long broadcastId
    ) {
        Member member = liveAuthUtils.getCurrentMember();
        broadcastService.likeBroadcast(broadcastId, member.getMemberId());
        return ResponseEntity.ok(ApiResult.success(null));
    }
}
