package com.deskit.deskit.product.repository;

import com.deskit.deskit.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
