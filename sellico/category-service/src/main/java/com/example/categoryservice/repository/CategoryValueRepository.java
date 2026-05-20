package com.example.categoryservice.repository;

import com.example.categoryservice.model.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryValueRepository extends JpaRepository<CategoryAttribute, UUID> {
}
