package com.deskit.deskit.livehost.service;

import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.repository.SellerRepository;
import com.deskit.deskit.livehost.common.enums.BroadcastProductStatus;
import com.deskit.deskit.livehost.common.enums.BroadcastStatus;
import com.deskit.deskit.livehost.common.enums.SanctionType;
import com.deskit.deskit.livehost.common.enums.VodStatus;
import com.deskit.deskit.livehost.common.exception.BusinessException;
import com.deskit.deskit.livehost.common.exception.ErrorCode;
import com.deskit.deskit.livehost.dto.request.BroadcastCreateRequest;
import com.deskit.deskit.livehost.dto.request.BroadcastProductRequest;
import com.deskit.deskit.livehost.dto.request.BroadcastSearch;
import com.deskit.deskit.livehost.dto.request.BroadcastUpdateRequest;
import com.deskit.deskit.livehost.dto.request.MediaConfigRequest;
import com.deskit.deskit.livehost.dto.request.OpenViduRecordingWebhook;
import com.deskit.deskit.livehost.dto.request.QcardRequest;
import com.deskit.deskit.livehost.dto.response.BroadcastAllResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastListResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastProductResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastResultResponse;
import com.deskit.deskit.livehost.dto.response.BroadcastStatsResponse;
import com.deskit.deskit.livehost.dto.response.MediaConfigResponse;
import com.deskit.deskit.livehost.dto.response.ProductSelectResponse;
import com.deskit.deskit.livehost.dto.response.QcardResponse;
import com.deskit.deskit.livehost.dto.response.ReservationSlotResponse;
import com.deskit.deskit.livehost.dto.response.StatisticsResponse;
import com.deskit.deskit.livehost.entity.Broadcast;
import com.deskit.deskit.livehost.entity.BroadcastProduct;
import com.deskit.deskit.livehost.entity.BroadcastResult;
import com.deskit.deskit.livehost.entity.Qcard;
import com.deskit.deskit.livehost.entity.Vod;
import com.deskit.deskit.livehost.repository.BroadcastProductRepository;
import com.deskit.deskit.livehost.repository.BroadcastRepository;
import com.deskit.deskit.livehost.repository.BroadcastRepositoryCustom;
import com.deskit.deskit.livehost.repository.BroadcastResultRepository;
import com.deskit.deskit.livehost.repository.SanctionRepository;
import com.deskit.deskit.livehost.repository.SanctionRepositoryCustom;
import com.deskit.deskit.livehost.repository.ViewHistoryRepository;
import com.deskit.deskit.livehost.repository.VodRepository;
import com.deskit.deskit.product.entity.Product;
import com.deskit.deskit.product.entity.Product.Status;
import com.deskit.deskit.product.repository.ProductRepository;
import com.deskit.deskit.tag.entity.TagCategory;
import com.deskit.deskit.tag.repository.TagCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastProductRepository broadcastProductRepository;
    private final com.deskit.deskit.livehost.repository.QcardRepository qcardRepository;
    private final BroadcastResultRepository broadcastResultRepository;
    private final VodRepository vodRepository;

    private final SellerRepository sellerRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final ProductRepository productRepository;
    private final SanctionRepository sanctionRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    private final RedisService redisService;
    private final SseService sseService;
    private final OpenViduService openViduService;
    private final AwsS3Service s3Service;
    private final DSLContext dsl;

    @Value("${openvidu.url}")
    private String openViduUrl;

    @Value("${openvidu.secret}")
    private String openViduSecret;

    @Transactional
    public Long createBroadcast(Long sellerId, BroadcastCreateRequest request) {
        String lockKey = "lock:seller:" + sellerId + ":broadcast_create";

        if (!Boolean.TRUE.equals(redisService.acquireLock(lockKey, 3000))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        try {
            long reservedCount = broadcastRepository.countBySellerIdAndStatus(sellerId, BroadcastStatus.RESERVED);
            if (reservedCount >= 7) {
                throw new BusinessException(ErrorCode.RESERVATION_LIMIT_EXCEEDED);
            }

            long slotCount = broadcastRepository.countByTimeSlot(request.getScheduledAt(), request.getScheduledAt().plusMinutes(30));
            if (slotCount >= 3) {
                throw new BusinessException(ErrorCode.BROADCAST_SLOT_FULL);
            }

            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
            TagCategory category = tagCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

            Broadcast broadcast = Broadcast.builder()
                    .seller(seller)
                    .tagCategory(category)
                    .broadcastTitle(request.getTitle())
                    .broadcastNotice(request.getNotice())
                    .scheduledAt(request.getScheduledAt())
                    .broadcastLayout(request.getBroadcastLayout())
                    .broadcastThumbUrl(request.getThumbnailUrl())
                    .broadcastWaitUrl(request.getWaitScreenUrl())
                    .status(BroadcastStatus.RESERVED)
                    .build();

            Broadcast saved = broadcastRepository.save(broadcast);
            saveBroadcastProducts(sellerId, saved, request.getProducts());
            saveQcards(saved, request.getQcards());

            log.info("방송 생성 완료: id={}", saved.getBroadcastId());
            return saved.getBroadcastId();
        } finally {
            redisService.releaseLock(lockKey);
        }
    }

    @Transactional
    public Long updateBroadcast(Long sellerId, Long broadcastId, BroadcastUpdateRequest request) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        TagCategory category = tagCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.RESERVED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            broadcast.updateBroadcastInfo(
                    category, request.getTitle(), request.getNotice(),
                    request.getScheduledAt(), request.getThumbnailUrl(),
                    request.getWaitScreenUrl(), request.getBroadcastLayout()
            );
            updateBroadcastProducts(sellerId, broadcast, request.getProducts());
            updateQcards(broadcast, request.getQcards());
        } else {
            broadcast.updateLiveBroadcastInfo(
                    category, request.getTitle(), request.getNotice(),
                    request.getThumbnailUrl(), request.getWaitScreenUrl()
            );
            sseService.notifyBroadcastUpdate(broadcastId);
        }

        return broadcast.getBroadcastId();
    }

    @Transactional
    public void cancelBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (broadcast.getStatus() != BroadcastStatus.RESERVED && broadcast.getStatus() != BroadcastStatus.READY) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        broadcast.cancelBroadcast("판매자 예약 취소");
        log.info("방송 취소 처리 완료: id={}, status={}", broadcastId, broadcast.getStatus());
    }

    @Transactional(readOnly = true)
    public List<ProductSelectResponse> getSellerProducts(Long sellerId, String keyword) {
        var productTable = table(name("product")).as("p");
        var productId = field(name("p", "product_id"), Long.class);
        var productName = field(name("p", "product_name"), String.class);
        var price = field(name("p", "price"), Integer.class);
        var stockQty = field(name("p", "stock_qty"), Integer.class);
        var sellerField = field(name("p", "seller_id"), Long.class);
        var statusField = field(name("p", "status"), String.class);
        var deletedAt = field(name("p", "deleted_at"), LocalDateTime.class);

        List<String> statuses = List.of(Status.ON_SALE.name(), Status.READY.name(), Status.LIMITED_SALE.name());

        var condition = sellerField.eq(sellerId)
                .and(statusField.in(statuses))
                .and(deletedAt.isNull());
        if (keyword != null && !keyword.isBlank()) {
            condition = condition.and(productName.containsIgnoreCase(keyword));
        }

        return dsl.select(productId, productName, price, stockQty)
                .from(productTable)
                .where(condition)
                .orderBy(productId.asc())
                .fetch(record -> ProductSelectResponse.builder()
                        .productId(record.get(productId))
                        .productName(record.get(productName))
                        .price(record.get(price))
                        .stockQty(record.get(stockQty))
                        .imageUrl(null)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<ReservationSlotResponse> getReservableSlots(LocalDate date) {
        LocalDateTime start = date.atTime(10, 0);
        LocalDateTime end = date.atTime(23, 0);

        var broadcastTable = table(name("broadcast")).as("b");
        var scheduledField = field(name("b", "scheduled_at"), LocalDateTime.class);
        var statusField = field(name("b", "status"), String.class);

        Map<LocalDateTime, Long> counts = dsl.select(scheduledField, org.jooq.impl.DSL.count())
                .from(broadcastTable)
                .where(
                        scheduledField.between(start, end),
                        statusField.in(BroadcastStatus.RESERVED.name(), BroadcastStatus.READY.name())
                )
                .groupBy(scheduledField)
                .fetchMap(scheduledField, record -> record.get(org.jooq.impl.DSL.count(), Long.class));

        List<ReservationSlotResponse> slots = new java.util.ArrayList<>();
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            int used = counts.getOrDefault(cursor, 0L).intValue();
            int remaining = Math.max(0, 3 - used);
            if (remaining > 0) {
                slots.add(ReservationSlotResponse.builder()
                        .slotDateTime(cursor)
                        .remainingCapacity(remaining)
                        .selectable(true)
                        .build());
            }
            cursor = cursor.plusMinutes(30);
        }
        return slots;
    }

    @Transactional
    public void saveMediaConfig(Long sellerId, Long broadcastId, MediaConfigRequest request) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        redisService.saveMediaConfig(
                broadcastId,
                sellerId,
                request.getCameraId(),
                request.getMicrophoneId(),
                request.getCameraOn(),
                request.getMicrophoneOn(),
                request.getVolume()
        );
    }

    @Transactional(readOnly = true)
    public MediaConfigResponse getMediaConfig(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        List<Object> values = redisService.getMediaConfig(broadcastId, sellerId);
        if (values == null || values.stream().allMatch(java.util.Objects::isNull)) {
            return MediaConfigResponse.builder()
                    .cameraId("")
                    .microphoneId("")
                    .cameraOn(true)
                    .microphoneOn(true)
                    .volume(50)
                    .build();
        }

        return MediaConfigResponse.builder()
                .cameraId(values.get(0) != null ? values.get(0).toString() : "")
                .microphoneId(values.get(1) != null ? values.get(1).toString() : "")
                .cameraOn(values.get(2) == null || Boolean.parseBoolean(values.get(2).toString()))
                .microphoneOn(values.get(3) == null || Boolean.parseBoolean(values.get(3).toString()))
                .volume(values.get(4) != null ? Integer.parseInt(values.get(4).toString()) : 50)
                .build();
    }

    @Transactional(readOnly = true)
    public BroadcastResponse getBroadcastDetail(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return createBroadcastResponse(broadcast);
    }

    @Transactional(readOnly = true)
    public BroadcastResponse getPublicBroadcastDetail(Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.DELETED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        }

        if (broadcast.getStatus() == BroadcastStatus.VOD) {
            Vod vod = vodRepository.findByBroadcast(broadcast)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOD_NOT_FOUND));
            if (vod.getStatus() != VodStatus.PUBLIC) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
            }
        }

        return createBroadcastResponse(broadcast);
    }

    @Transactional(readOnly = true)
    public Object getPublicBroadcasts(BroadcastSearch condition, Pageable pageable) {
        if ("ALL".equalsIgnoreCase(condition.getTab())) {
            return getOverview(null, false);
        }
        Slice<BroadcastListResponse> list = broadcastRepository.searchBroadcasts(null, condition, pageable, false);
        injectLiveStats(list.getContent());
        return list;
    }

    @Transactional(readOnly = true)
    public Object getSellerBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable) {
        if ("ALL".equalsIgnoreCase(condition.getTab())) {
            return getOverview(sellerId, true);
        }
        Slice<BroadcastListResponse> list = broadcastRepository.searchBroadcasts(sellerId, condition, pageable, true);
        injectLiveStats(list.getContent());
        return list;
    }

    @Transactional(readOnly = true)
    public Object getAdminBroadcasts(BroadcastSearch condition, Pageable pageable) {
        return broadcastRepository.searchBroadcasts(null, condition, pageable, true);
    }

    @Transactional
    public String startBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (broadcast.getScheduledAt() != null && LocalDateTime.now().isBefore(broadcast.getScheduledAt())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        broadcast.startBroadcast("session-" + broadcastId);

        try {
            Map<String, Object> params = Map.of("role", "HOST", "sellerId", sellerId);
            return openViduService.createToken(broadcastId, params);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPENVIDU_ERROR);
        }
    }

    public String joinBroadcast(Long broadcastId, String viewerId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.STOPPED) {
            throw new BusinessException(ErrorCode.BROADCAST_STOPPED_BY_ADMIN);
        }
        if (!isLiveGroup(broadcast.getStatus())) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_ON_AIR);
        }

        Long memberId = parseMemberId(viewerId);
        if (memberId != null && isViewerSanctioned(broadcastId, memberId)) {
            throw new BusinessException(ErrorCode.BROADCAST_ALREADY_SANCTIONED);
        }

        String uuid = (viewerId != null) ? viewerId : UUID.randomUUID().toString();
        redisService.enterLiveRoom(broadcastId, uuid);

        try {
            Map<String, Object> params = Map.of("role", "SUBSCRIBER");
            return openViduService.createToken(broadcastId, params);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPENVIDU_ERROR);
        }
    }

    @Transactional
    public void endBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        broadcast.endBroadcast();
        openViduService.closeSession(broadcastId);
        sseService.notifyBroadcastUpdate(broadcastId, "BROADCAST_ENDED", "ended");
    }

    @Transactional
    public void pinProduct(Long sellerId, Long broadcastId, Long bpId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));
        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        broadcastProductRepository.resetPinByBroadcastId(broadcastId);

        BroadcastProduct bp = broadcastProductRepository.findById(bpId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!bp.getBroadcast().getBroadcastId().equals(broadcastId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        bp.setPinned(true);

        sseService.notifyBroadcastUpdate(broadcastId, "PRODUCT_PINNED", bp.getProduct().getId());
    }

    @EventListener
    public void handleConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String bId = accessor.getFirstNativeHeader("broadcastId");
        String vId = accessor.getFirstNativeHeader("X-Viewer-Id");
        if (bId != null && vId != null) {
            redisService.enterLiveRoom(Long.parseLong(bId), vId);
            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs != null) {
                attrs.put("broadcastId", bId);
                attrs.put("viewerId", vId);
            }
        }
    }

    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.containsKey("broadcastId")) {
            redisService.exitLiveRoom(Long.parseLong((String) attrs.get("broadcastId")), (String) attrs.get("viewerId"));
        }
    }

    public void likeBroadcast(Long broadcastId, Long memberId) {
        redisService.toggleLike(broadcastId, memberId);
    }

    @Transactional
    public void processVod(OpenViduRecordingWebhook payload) {
        Long broadcastId = Long.parseLong(payload.getSessionId().replace("broadcast-", ""));
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        String recordingId = payload.getId();
        String s3Key = "seller_" + broadcast.getSeller().getSellerId() + "/vods/" + recordingId + ".mp4";
        String s3Url = payload.getUrl() != null ? payload.getUrl() : "";

        s3Url = uploadVodWithRetry(recordingId, s3Key, s3Url);

        boolean isStopped = broadcast.getStatus() == BroadcastStatus.STOPPED;
        VodStatus status = isStopped ? VodStatus.PRIVATE : VodStatus.PUBLIC;

        long vodSize = payload.getSize() != null ? payload.getSize() : 0L;
        if (vodSize == 0L && s3Url != null && !s3Url.isBlank()) {
            vodSize = s3Service.getObjectSize(s3Url);
        }

        Vod vod = Vod.builder()
                .broadcast(broadcast)
                .vodUrl(s3Url)
                .vodSize(vodSize)
                .vodDuration(payload.getDuration() != null ? payload.getDuration().intValue() : 0)
                .status(status)
                .vodReportCount(0)
                .vodAdminLock(isStopped)
                .build();
        vodRepository.save(vod);

        int uv = redisService.getTotalUniqueViewerCount(broadcastId);
        int likes = redisService.getLikeCount(broadcastId);
        int reports = redisService.getReportCount(broadcastId);
        int mv = redisService.getMaxViewers(broadcastId);
        LocalDateTime peak = redisService.getMaxViewersTime(broadcastId);
        Double avg = viewHistoryRepository.getAverageWatchTime(broadcastId);

        BroadcastResult result = BroadcastResult.builder()
                .broadcast(broadcast)
                .totalViews(uv)
                .totalLikes(likes)
                .totalReports(reports)
                .avgWatchTime(avg != null ? avg.intValue() : 0)
                .maxViews(mv)
                .pickViewsAt(peak)
                .totalChats(0)
                .totalSales(BigDecimal.ZERO)
                .build();
        broadcastResultRepository.save(result);

        redisService.deleteBroadcastKeys(broadcastId);
        if (isStopped || broadcast.getStatus() == BroadcastStatus.ENDED) {
            broadcast.changeStatus(BroadcastStatus.VOD);
        }
    }

    private String uploadVodWithRetry(String recordingId, String s3Key, String fallbackUrl) {
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            try {
                disableSslVerification();

                String videoUrl = openViduUrl.replaceAll("/$", "") +
                        "/openvidu/recordings/" + recordingId + "/" + recordingId + ".mp4";

                URL url = new URL(videoUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                String auth = "OPENVIDUAPP:" + openViduSecret;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream inputStream = conn.getInputStream()) {
                        long contentLength = conn.getContentLengthLong();
                        String s3Url = s3Service.uploadVodStream(inputStream, s3Key, contentLength);
                        log.info("VOD Upload Success: {}", s3Url);
                        return s3Url;
                    }
                } else {
                    log.error("Failed to fetch recording from OpenVidu: {}", responseCode);
                }
            } catch (Exception e) {
                log.error("VOD Processing Error (attempt {}): {}", attempts, e.getMessage());
            }

            try {
                Thread.sleep(1000L * attempts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return fallbackUrl;
    }

    @Transactional(readOnly = true)
    public BroadcastStatsResponse getBroadcastStats(Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.DELETED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        }

        int views = 0;
        int likes = 0;
        int reports = 0;

        if (isLiveGroup(broadcast.getStatus())) {
            views = redisService.getRealtimeViewerCount(broadcastId);
            likes = redisService.getLikeCount(broadcastId);
            reports = redisService.getReportCount(broadcastId);
        } else {
            BroadcastResult result = broadcastResultRepository.findById(broadcastId).orElse(null);
            if (result != null) {
                views = result.getTotalViews();
                likes = result.getTotalLikes();
                reports = sanctionRepository.countByBroadcast(broadcast);
            }
        }

        return BroadcastStatsResponse.builder()
                .viewerCount(views)
                .likeCount(likes)
                .reportCount(reports)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BroadcastProductResponse> getBroadcastProducts(Long broadcastId) {
        return broadcastProductRepository.findAllWithProductByBroadcastId(broadcastId).stream()
                .map(BroadcastProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean canChat(Long broadcastId, Long memberId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.STOPPED) {
            return false;
        }

        if (memberId == null) {
            return true;
        }

        return !isViewerSanctioned(broadcastId, memberId, SanctionType.MUTE, SanctionType.OUT);
    }

    public void reportBroadcast(Long broadcastId, Long memberId) {
        if (!broadcastRepository.existsById(broadcastId)) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        }
        redisService.reportBroadcast(broadcastId, memberId);
    }

    @Transactional(readOnly = true)
    public BroadcastResultResponse getBroadcastResult(Long broadcastId, Long requesterId, boolean isAdmin) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!isAdmin && !broadcast.getSeller().getSellerId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        BroadcastResult result = broadcastResultRepository.findById(broadcastId).orElse(null);
        Vod vod = vodRepository.findByBroadcast(broadcast).orElse(null);

        int views = 0;
        int likes = 0;
        int chats = 0;
        int maxV = 0;
        int reports = 0;
        int sanctions;
        long avgTime = 0;
        BigDecimal sales = BigDecimal.ZERO;
        LocalDateTime maxTime = null;

        if (result != null) {
            views = result.getTotalViews();
            likes = result.getTotalLikes();
            sales = result.getTotalSales();
            chats = result.getTotalChats();
            maxV = result.getMaxViews();
            maxTime = result.getPickViewsAt();
            avgTime = result.getAvgWatchTime();
            reports = result.getTotalReports();
        }
        sanctions = sanctionRepository.countByBroadcast(broadcast);

        List<BroadcastResultResponse.ProductSalesStat> productStats = broadcastProductRepository
                .findAllWithProductByBroadcastId(broadcastId)
                .stream()
                .map(bp -> BroadcastResultResponse.ProductSalesStat.builder()
                        .productId(bp.getProduct().getId())
                        .productName(bp.getProduct().getProductName())
                        .salesAmount(BigDecimal.ZERO)
                        .price(bp.getBpPrice())
                        .salesQuantity(0)
                        .build())
                .collect(Collectors.toList());

        long duration = 0;
        if (broadcast.getStartedAt() != null && broadcast.getEndedAt() != null) {
            duration = java.time.Duration.between(broadcast.getStartedAt(), broadcast.getEndedAt()).toMinutes();
        }

        return BroadcastResultResponse.builder()
                .broadcastId(broadcast.getBroadcastId())
                .title(broadcast.getBroadcastTitle())
                .startAt(broadcast.getStartedAt())
                .endAt(broadcast.getEndedAt())
                .durationMinutes(duration)
                .status(broadcast.getStatus())
                .stoppedReason(broadcast.getBroadcastStoppedReason())
                .totalViews(views)
                .totalLikes(likes)
                .totalSales(sales)
                .totalChats(chats)
                .maxViewers(maxV)
                .maxViewerTime(maxTime)
                .avgWatchTime(avgTime)
                .reportCount(reports)
                .sanctionCount(sanctions)
                .vodUrl((vod != null && vod.getStatus() != VodStatus.DELETED) ? vod.getVodUrl() : null)
                .vodStatus(vod != null ? vod.getStatus() : null)
                .isEncoding(vod == null)
                .productStats(productStats)
                .build();
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(Long sellerId, String period) {
        var sales = broadcastResultRepository.getSalesChart(sellerId, period);
        var arpu = broadcastResultRepository.getArpuChart(sellerId, period);
        List<StatisticsResponse.BroadcastRank> best;
        List<StatisticsResponse.BroadcastRank> worst;
        List<StatisticsResponse.BroadcastRank> topView;

        if (sellerId != null) {
            best = broadcastResultRepository.getRanking(sellerId, period, "SALES", true, 5);
            worst = broadcastResultRepository.getRanking(sellerId, period, "SALES", false, 5);
            topView = broadcastResultRepository.getRanking(sellerId, period, "VIEWS", true, 5);
        } else {
            best = broadcastResultRepository.getRanking(null, period, "SALES", true, 10);
            worst = broadcastResultRepository.getRanking(null, period, "SALES", false, 10);
            topView = List.of();
        }

        return StatisticsResponse.builder()
                .salesChart(sales)
                .arpuChart(arpu)
                .bestBroadcasts(best)
                .worstBroadcasts(worst)
                .topViewerBroadcasts(topView)
                .build();
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void syncBroadcastSchedules() {
        LocalDateTime now = LocalDateTime.now();

        List<Long> readyTargets = broadcastRepository.findBroadcastIdsForReadyTransition(now);
        for (Long broadcastId : readyTargets) {
            Broadcast broadcast = broadcastRepository.findById(broadcastId).orElse(null);
            if (broadcast != null && broadcast.getStatus() == BroadcastStatus.RESERVED) {
                broadcast.readyBroadcast();
                sseService.notifyBroadcastUpdate(broadcastId, "BROADCAST_READY", "ready");
            }
        }

        List<Long> noShowTargets = broadcastRepository.findBroadcastIdsForNoShow(now);
        for (Long broadcastId : noShowTargets) {
            Broadcast broadcast = broadcastRepository.findById(broadcastId).orElse(null);
            if (broadcast != null && (broadcast.getStatus() == BroadcastStatus.RESERVED || broadcast.getStatus() == BroadcastStatus.READY)) {
                broadcast.markNoShow("방송 시작 시간 초과");
                sseService.notifyBroadcastUpdate(broadcastId, "BROADCAST_CANCELED", "no_show");
            }
        }

        List<BroadcastRepositoryCustom.BroadcastScheduleInfo> schedules = broadcastRepository.findBroadcastSchedules(
                now.minusHours(2),
                now.plusHours(2),
                List.of(BroadcastStatus.ON_AIR, BroadcastStatus.READY, BroadcastStatus.ENDED)
        );

        for (BroadcastRepositoryCustom.BroadcastScheduleInfo schedule : schedules) {
            if (schedule.scheduledAt() == null) {
                continue;
            }
            LocalDateTime scheduledEnd = schedule.scheduledAt().plusMinutes(60);
            if (!scheduledEnd.isAfter(now)) {
                String noticeKey = redisService.getScheduleNoticeKey(schedule.broadcastId(), "ended");
                if (redisService.setIfAbsent(noticeKey, "sent", java.time.Duration.ofHours(2))) {
                    Broadcast broadcast = broadcastRepository.findById(schedule.broadcastId()).orElse(null);
                    if (broadcast != null && broadcast.getStatus() == BroadcastStatus.ON_AIR) {
                        broadcast.endBroadcast();
                        openViduService.closeSession(schedule.broadcastId());
                    }
                    sseService.notifyBroadcastUpdate(schedule.broadcastId(), "BROADCAST_SCHEDULED_END", "ended");
                }
                continue;
            }

            LocalDateTime noticeAt = scheduledEnd.minusMinutes(1);
            if (!noticeAt.isAfter(now)) {
                String noticeKey = redisService.getScheduleNoticeKey(schedule.broadcastId(), "ending_soon");
                if (redisService.setIfAbsent(noticeKey, "sent", java.time.Duration.ofHours(2))) {
                    sseService.notifyBroadcastUpdate(schedule.broadcastId(), "BROADCAST_ENDING_SOON", "1m");
                }
            }
        }
    }

    private void saveBroadcastProducts(Long sellerId, Broadcast broadcast, List<BroadcastProductRequest> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        int order = 1;
        for (BroadcastProductRequest dto : products) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            if (!product.getSellerId().equals(sellerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
            }

            BroadcastProduct bp = BroadcastProduct.builder()
                    .broadcast(broadcast)
                    .product(product)
                    .bpPrice(dto.getBpPrice())
                    .bpQuantity(dto.getBpQuantity())
                    .displayOrder(order++)
                    .isPinned(false)
                    .status(BroadcastProductStatus.SELLING)
                    .build();
            broadcastProductRepository.save(bp);
        }
    }

    private void saveQcards(Broadcast broadcast, List<QcardRequest> qcards) {
        if (qcards == null || qcards.isEmpty()) {
            return;
        }

        int sortOrder = 1;
        for (QcardRequest dto : qcards) {
            Qcard qcard = Qcard.builder()
                    .broadcast(broadcast)
                    .qcardQuestion(dto.getQuestion())
                    .sortOrder(sortOrder++)
                    .build();

            qcardRepository.save(qcard);
        }
    }

    private void updateBroadcastProducts(Long sellerId, Broadcast broadcast, List<BroadcastProductRequest> products) {
        broadcastProductRepository.deleteByBroadcast(broadcast);
        saveBroadcastProducts(sellerId, broadcast, products);
    }

    private void updateQcards(Broadcast broadcast, List<QcardRequest> qcards) {
        qcardRepository.deleteByBroadcast(broadcast);
        saveQcards(broadcast, qcards);
    }

    private List<BroadcastProductResponse> getProductListResponse(Broadcast broadcast) {
        return broadcast.getProducts().stream()
                .map(BroadcastProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private List<QcardResponse> getQcardListResponse(Broadcast broadcast) {
        return broadcast.getQcards().stream()
                .map(q -> QcardResponse.builder()
                        .question(q.getQcardQuestion())
                        .sortOrder(q.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isLiveGroup(BroadcastStatus status) {
        return status == BroadcastStatus.ON_AIR || status == BroadcastStatus.READY || status == BroadcastStatus.ENDED;
    }

    private Long parseMemberId(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(viewerId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isViewerSanctioned(Long broadcastId, Long memberId, SanctionType... types) {
        SanctionRepositoryCustom.SanctionTypeResult result = sanctionRepository.findLatestSanction(broadcastId, memberId);
        if (result == null || result.status() == null) {
            return false;
        }
        for (SanctionType type : types) {
            if (type.name().equalsIgnoreCase(result.status())) {
                return true;
            }
        }
        return false;
    }

    private boolean isViewerSanctioned(Long broadcastId, Long memberId) {
        return isViewerSanctioned(broadcastId, memberId, SanctionType.OUT);
    }

    private BroadcastResponse createBroadcastResponse(Broadcast broadcast) {
        Integer views = 0;
        Integer likes = 0;
        Integer reports = 0;
        String vodUrl = null;

        if (isLiveGroup(broadcast.getStatus())) {
            views = redisService.getRealtimeViewerCount(broadcast.getBroadcastId());
            likes = redisService.getLikeCount(broadcast.getBroadcastId());
            reports = redisService.getReportCount(broadcast.getBroadcastId());
        } else {
            BroadcastResult result = broadcastResultRepository.findById(broadcast.getBroadcastId()).orElse(null);
            if (result != null) {
                views = result.getTotalViews();
                likes = result.getTotalLikes();
                reports = result.getTotalReports();
            }
        }

        if (broadcast.getStatus() == BroadcastStatus.VOD) {
            Vod vod = vodRepository.findByBroadcast(broadcast).orElse(null);
            if (vod != null && vod.getStatus() == VodStatus.PUBLIC) {
                vodUrl = vod.getVodUrl();
            }
        }

        return BroadcastResponse.fromEntity(
                broadcast,
                broadcast.getTagCategory().getTagCategoryName(),
                views,
                likes,
                reports,
                getProductListResponse(broadcast),
                getQcardListResponse(broadcast),
                vodUrl
        );
    }

    private BroadcastAllResponse getOverview(Long sellerId, boolean isAdmin) {
        List<BroadcastListResponse> onAir = broadcastRepository.findTop5ByStatus(
                sellerId,
                List.of(BroadcastStatus.ON_AIR, BroadcastStatus.READY),
                BroadcastRepositoryCustom.BroadcastSortOrder.STARTED_AT_DESC,
                isAdmin
        );
        List<BroadcastListResponse> reserved = broadcastRepository.findTop5ByStatus(
                sellerId,
                List.of(BroadcastStatus.RESERVED),
                BroadcastRepositoryCustom.BroadcastSortOrder.SCHEDULED_AT_ASC,
                isAdmin
        );
        List<BroadcastListResponse> vod = broadcastRepository.findTop5ByStatus(
                sellerId,
                List.of(BroadcastStatus.VOD, BroadcastStatus.ENDED, BroadcastStatus.STOPPED),
                BroadcastRepositoryCustom.BroadcastSortOrder.ENDED_AT_DESC,
                isAdmin
        );
        injectLiveDetails(onAir);
        return BroadcastAllResponse.builder().onAir(onAir).reserved(reserved).vod(vod).build();
    }

    private void injectLiveStats(List<BroadcastListResponse> list) {
        list.forEach(item -> {
            if (item.getStatus() == BroadcastStatus.ON_AIR) {
                item.setLiveViewerCount(redisService.getRealtimeViewerCount(item.getBroadcastId()));
                item.setTotalLikes(redisService.getLikeCount(item.getBroadcastId()));
                item.setReportCount(redisService.getReportCount(item.getBroadcastId()));
            }
        });
    }

    private void injectLiveDetails(List<BroadcastListResponse> list) {
        list.forEach(item -> {
            if (item.getStatus() == BroadcastStatus.ON_AIR) {
                item.setLiveViewerCount(redisService.getRealtimeViewerCount(item.getBroadcastId()));
                item.setTotalLikes(redisService.getLikeCount(item.getBroadcastId()));
                item.setReportCount(redisService.getReportCount(item.getBroadcastId()));

                List<BroadcastProduct> products = broadcastProductRepository.findAllWithProductByBroadcastId(item.getBroadcastId());

                item.setProducts(products.stream().map(bp -> {
                    Product p = bp.getProduct();
                    return BroadcastListResponse.SimpleProductInfo.builder()
                            .name(p.getProductName())
                            .stock(bp.getBpQuantity())
                            .isSoldOut(bp.getBpQuantity() <= 0)
                            .build();
                }).collect(Collectors.toList()));
            }
        });
    }

    private void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            log.warn("SSL verification disable failed: {}", e.getMessage());
        }
    }
}
