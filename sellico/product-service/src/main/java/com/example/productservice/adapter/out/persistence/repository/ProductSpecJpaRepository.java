package com.example.productservice.adapter.out.persistence.repository;

import com.example.productservice.adapter.out.persistence.entity.ProductSpecificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductSpecJpaRepository extends JpaRepository<ProductSpecificationEntity, UUID> {
}
