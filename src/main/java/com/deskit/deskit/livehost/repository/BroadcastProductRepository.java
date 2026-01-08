package com.deskit.deskit.livehost.repository;

import com.deskit.deskit.livehost.entity.Broadcast;
import com.deskit.deskit.livehost.entity.BroadcastProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT bp FROM BroadcastProduct bp " +
            "JOIN FETCH bp.product p " +
            "WHERE bp.broadcast.broadcastId = :broadcastId " +
            "ORDER BY bp.displayOrder ASC")
    List<BroadcastProduct> findAllWithProductByBroadcastId(@Param("broadcastId") Long broadcastId);
}
