package com.deskit.deskit.livehost.repository;

import com.deskit.deskit.livehost.entity.Sanction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {
}
