package com.deskit.deskit.setup.repository;

import com.deskit.deskit.setup.entity.Setup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetupRepository extends JpaRepository<Setup, Long> {

  List<Setup> findAllByDeletedAtIsNullOrderByIdAsc();

  Optional<Setup> findByIdAndDeletedAtIsNull(Long id);

  @Query(value = """
      select sp.product_id
      from setup_product sp
      where sp.setup_id = :setupId
      """, nativeQuery = true)
  List<Long> findProductIdsBySetupId(@Param("setupId") Long setupId);
}
