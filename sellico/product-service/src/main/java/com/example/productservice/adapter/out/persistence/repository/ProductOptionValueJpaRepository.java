package com.example.productservice.adapter.out.persistence.repository;

import com.example.productservice.adapter.out.persistence.entity.ProductOptionValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductOptionValueJpaRepository extends JpaRepository<ProductOptionValueEntity, UUID> {
}
