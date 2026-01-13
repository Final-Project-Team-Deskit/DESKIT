package com.deskit.deskit.livehost.repository;

import com.deskit.deskit.livehost.entity.Broadcast;
import com.deskit.deskit.livehost.entity.BroadcastProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BroadcastProductRepository extends JpaRepository<BroadcastProduct, Long> {
    void deleteByBroadcast(Broadcast broadcast);

    @Modifying
    @Query("UPDATE BroadcastProduct bp SET bp.isPinned = false WHERE bp.broadcast.broadcastId = :broadcastId")
    void resetPinByBroadcastId(@Param("broadcastId") Long broadcastId);

    @Query("SELECT bp FROM BroadcastProduct bp WHERE bp.broadcast.broadcastId = :bid AND bp.product.id = :pid")
    Optional<BroadcastProduct> findByBroadcastIdAndProductId(@Param("bid") Long bid, @Param("pid") Long pid);

    @Query("SELECT bp.bpQuantity FROM BroadcastProduct bp WHERE bp.broadcast.broadcastId = :bid AND bp.product.id = :pid")
    Integer findStock(@Param("bid") Long bid, @Param("pid") Long pid);

    @Query("""
            SELECT DISTINCT bp.broadcast.broadcastId
            FROM BroadcastProduct bp
            WHERE bp.product.id IN :productIds
              AND bp.broadcast.startedAt IS NOT NULL
              AND :paidAt >= bp.broadcast.startedAt
              AND (bp.broadcast.endedAt IS NULL OR :paidAt <= bp.broadcast.endedAt)
            """)
    List<Long> findBroadcastIdsByProductIdsAndPaidAt(
            @Param("productIds") List<Long> productIds,
            @Param("paidAt") LocalDateTime paidAt
    );

    @Query("SELECT bp FROM BroadcastProduct bp " +
            "JOIN FETCH bp.product p " +
            "WHERE bp.broadcast.broadcastId = :broadcastId " +
            "ORDER BY bp.displayOrder ASC")
    List<BroadcastProduct> findAllWithProductByBroadcastId(@Param("broadcastId") Long broadcastId);

    @Query("SELECT bp FROM BroadcastProduct bp " +
            "JOIN FETCH bp.product p " +
            "WHERE bp.broadcast.broadcastId IN :broadcastIds " +
            "ORDER BY bp.broadcast.broadcastId ASC, bp.displayOrder ASC")
    List<BroadcastProduct> findAllWithProductByBroadcastIdIn(@Param("broadcastIds") List<Long> broadcastIds);
}
