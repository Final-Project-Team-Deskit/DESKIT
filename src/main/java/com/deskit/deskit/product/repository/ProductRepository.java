package com.deskit.deskit.product.repository;

import com.deskit.deskit.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

  List<Product> findAllByDeletedAtIsNullOrderByIdAsc();

  Optional<Product> findByIdAndDeletedAtIsNull(Long id);
}
