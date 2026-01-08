package com.deskit.deskit.livehost.service;

import com.deskit.deskit.livehost.common.enums.BroadcastStatus;
import com.deskit.deskit.livehost.common.exception.BusinessException;
import com.deskit.deskit.livehost.common.exception.ErrorCode;
import com.deskit.deskit.livehost.dto.response.SanctionStatisticsResponse;
import com.deskit.deskit.livehost.entity.Broadcast;
import com.deskit.deskit.livehost.repository.BroadcastRepository;
import com.deskit.deskit.livehost.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final SanctionRepository sanctionRepository;
    private final BroadcastRepository broadcastRepository;
    private final OpenViduService openViduService;
    private final RedisService redisService;
    private final SseService sseService;
    private final SanctionService sanctionService;

    @Transactional(readOnly = true)
    public SanctionStatisticsResponse getSanctionStatistics(String period) {
        return SanctionStatisticsResponse.builder()
                .forceStopChart(sanctionRepository.getSellerForceStopChart(period))
                .viewerBanChart(sanctionRepository.getViewerSanctionChart(period))
                .worstSellers(sanctionRepository.getSellerForceStopRanking(period, 5))
                .worstViewers(sanctionRepository.getViewerSanctionRanking(period, 5))
                .build();
    }

    @Transactional
    public void forceStopBroadcast(Long broadcastId, String reason) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        broadcast.forceStopByAdmin(reason);

        openViduService.closeSession(broadcastId);
        redisService.deleteBroadcastKeys(broadcastId);
        sseService.notifyBroadcastUpdate(broadcastId, "BROADCAST_STOPPED", reason);
    }

    @Transactional
    public void cancelBroadcast(Long broadcastId, String reason) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() != BroadcastStatus.RESERVED && broadcast.getStatus() != BroadcastStatus.READY) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        broadcast.cancelBroadcast(reason);
    }

    @Transactional
    public void sanctionViewer(Long adminId, Long broadcastId, com.deskit.deskit.livehost.dto.request.SanctionRequest request) {
        sanctionService.sanctionUserByAdmin(adminId, broadcastId, request);
    }
}
