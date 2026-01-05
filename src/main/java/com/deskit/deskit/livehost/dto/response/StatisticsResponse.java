package com.deskit.deskit.livehost.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsResponse {
    private List<ChartData> salesChart;      // 매출 차트
    private List<ChartData> arpuChart;       // 객단가 차트

    private List<BroadcastRank> bestBroadcasts;  // 매출 Best
    private List<BroadcastRank> worstBroadcasts; // 매출 Worst
    private List<BroadcastRank> topViewerBroadcasts; // 시청자수 Best (판매자용)

    @Getter
    @AllArgsConstructor
    public static class ChartData {
        private String label;
        private BigDecimal value;
    }
    @Getter
    @Builder
    public static class BroadcastRank {
        private Long broadcastId;
        private String title;
        private BigDecimal totalSales;
        private int totalViews;
    }
}
