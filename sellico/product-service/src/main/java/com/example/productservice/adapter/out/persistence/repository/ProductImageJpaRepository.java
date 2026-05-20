package com.example.productservice.adapter.out.persistence.repository;

import com.example.productservice.adapter.out.persistence.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductImageJpaRepository extends JpaRepository<ProductImageEntity, UUID> {
}
