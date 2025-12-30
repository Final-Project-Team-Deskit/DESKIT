package com.deskit.deskit.setup.repository;

import com.deskit.deskit.setup.entity.Setup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetupRepository extends JpaRepository<Setup, Long> {

  List<Setup> findAllByDeletedAtIsNullOrderByIdAsc();

  Optional<Setup> findByIdAndDeletedAtIsNull(Long id);
}
