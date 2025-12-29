package com.deskit.deskit.product.repository;

import com.deskit.deskit.product.entity.ProductTag;
import com.deskit.deskit.product.entity.ProductTag.ProductTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {
}
