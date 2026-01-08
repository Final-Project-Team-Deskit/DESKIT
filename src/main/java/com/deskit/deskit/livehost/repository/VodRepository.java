package com.deskit.deskit.livehost.repository;

import com.deskit.deskit.livehost.entity.Broadcast;
import com.deskit.deskit.livehost.entity.Vod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VodRepository extends JpaRepository<Vod, Long> {
    Optional<Vod> findByBroadcast(Broadcast broadcast);
}
