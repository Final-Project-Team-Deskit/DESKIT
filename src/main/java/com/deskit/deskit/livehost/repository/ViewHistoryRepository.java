package com.deskit.deskit.livehost.repository;

import com.deskit.deskit.livehost.entity.Broadcast;
import com.deskit.deskit.livehost.entity.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    @Query("SELECT v FROM ViewHistory v WHERE v.broadcast = :broadcast AND v.viewerId = :viewerId AND v.updatedAt = v.createdAt ORDER BY v.createdAt DESC LIMIT 1")
    Optional<ViewHistory> findActiveHistory(@Param("broadcast") Broadcast broadcast, @Param("viewerId") String viewerId);

    @Query(value = "SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, v.created_at, v.updated_at)), 0) " +
            "FROM view_history v " +
            "WHERE v.broadcast_id = :broadcastId",
            nativeQuery = true)
    Double getAverageWatchTime(@Param("broadcastId") Long broadcastId);

    @Modifying
    @Query("DELETE FROM ViewHistory v WHERE v.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
