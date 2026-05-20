package com.example.productservice.adapter.out.persistence;

import com.example.productservice.adapter.out.persistence.entity.ProductEntity;
import com.example.productservice.adapter.out.persistence.entity.ProductSpecificationEntity;
import com.example.productservice.adapter.out.persistence.mapper.ProductImageMapper;
import com.example.productservice.adapter.out.persistence.mapper.ProductMapper;
import com.example.productservice.adapter.out.persistence.repository.ProductJpaRepository;
import com.example.productservice.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.example.productservice.adapter.out.persistence.repository.ProductSpecJpaRepository;
import com.example.productservice.adapter.out.persistence.specification.ProductSpecification;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.model.Product;
import model.SpecAttribute;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ProductJpaAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository productRepo;
    private final ProductSpecJpaRepository specRepo;
    private final ProductSkuJpaRepository skuRepo;
    private final EntityManager entityManager;

    private final ProductMapper mapper;
    private final ProductImageMapper imageMapper;

    @Override
    public boolean existsBySellerIdAndSlug(UUID sellerId, String slug) {
        return productRepo.existsBySellerIdAndSlug(sellerId, slug);
    }

    @Override
    public Set<String> findExistingSkuCodesBySellerId(UUID sellerId, Collection<String> skuCodes) {
        return skuRepo.findExistingSkuCodesBySellerId(sellerId, skuCodes);
    }

    @Override
    @Transactional
    public Product save(Product domainModel) {
        if (domainModel.getId() != null) {
            return update(domainModel);
        }
        // New product: cascade persist works fine for all children including @MapsId spec
        ProductEntity entity = mapper.toEntity(domainModel);
        ProductEntity savedEntity = productRepo.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public Product updateProductOnly(Product domainModel) {
        ProductEntity entity = productRepo.findById(domainModel.getId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + domainModel.getId()));

        // Update only the product-level fields, leave children untouched
        entity.setName(domainModel.getName());
        entity.setSlug(domainModel.getSlug());
        entity.setDescription(domainModel.getDescription());
        entity.setStatus(domainModel.getStatus());
        entity.setCategoryId(domainModel.getCategoryId());
        entity.setMinPrice(domainModel.getMinPrice());
        entity.setMaxPrice(domainModel.getMaxPrice());
        entity.setRatingAvg(domainModel.getRatingAvg());
        entity.setRatingCount(domainModel.getRatingCount());
        entity.setSoldCount(domainModel.getSoldCount());
        entity.setIsDeleted(domainModel.getIsDeleted());
        entity.setUpdatedAt(domainModel.getUpdatedAt());

        ProductEntity saved = productRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public Product updateSpecifications(UUID productId, List<SpecAttribute> specifications) {
        ProductEntity entity = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Update spec via managed entity — no delete/reinsert needed
        Optional<ProductSpecificationEntity> existingSpec = specRepo.findById(productId);
        if (existingSpec.isPresent()) {
            existingSpec.get().setAttributes(specifications);
            specRepo.save(existingSpec.get());
        } else {
            ProductSpecificationEntity newSpec = new ProductSpecificationEntity();
            newSpec.setAttributes(specifications);
            newSpec.setProduct(entity);
            specRepo.save(newSpec);
        }

        entity.setUpdatedAt(java.time.Instant.now());
        productRepo.save(entity);

        // Reload to include spec in response
        entityManager.flush();
        entityManager.clear();
        return productRepo.findById(productId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Product not found after save: " + productId));
    }

    /**
     * Handle updates separately to work around two Hibernate issues:
     * <p>
     * 1) NonUniqueObjectException for @MapsId ProductSpecificationEntity:
     *    mapper.toEntity() creates a new spec with the same PK as the managed one.
     * 2) ConstraintViolationException on product_skus.sku_code:
     *    merge() + orphanRemoval does INSERTs before DELETEs, violating UNIQUE.
     * <p>
     * Fix: delete all child rows via native SQL first, clear the persistence
     * context, then merge the product (children become clean INSERTs).
     * Spec is handled separately because of @MapsId.
     */
    private Product update(Product domainModel) {
        // Build entity graph from domain model
        ProductEntity entity = mapper.toEntity(domainModel);

        // Extract spec info for separate handling (@MapsId)
        List<SpecAttribute> specAttributes = null;
        if (entity.getSpec() != null) {
            specAttributes = entity.getSpec().getAttributes();
        }
        entity.setSpec(null);

        UUID pid = domainModel.getId();

        // Delete child rows in FK-safe order via native SQL
        entityManager.createNativeQuery(
                "DELETE FROM sku_attribute_values WHERE sku_id IN (SELECT id FROM product_skus WHERE product_id = ?1)")
                .setParameter(1, pid).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM product_skus WHERE product_id = ?1")
                .setParameter(1, pid).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM product_option_values WHERE option_id IN (SELECT id FROM product_options WHERE product_id = ?1)")
                .setParameter(1, pid).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM product_options WHERE product_id = ?1")
                .setParameter(1, pid).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM product_images WHERE product_id = ?1")
                .setParameter(1, pid).executeUpdate();

        // Clear persistence context — old managed entities reference deleted rows
        entityManager.flush();
        entityManager.clear();

        // Merge product; all children are fresh INSERTs (no conflict)
        ProductEntity saved = productRepo.save(entity);

        // Handle @MapsId spec separately
        if (specAttributes != null && !specAttributes.isEmpty()) {
            Optional<ProductSpecificationEntity> existingSpec = specRepo.findById(saved.getId());
            if (existingSpec.isPresent()) {
                existingSpec.get().setAttributes(specAttributes);
                specRepo.save(existingSpec.get());
            } else {
                ProductSpecificationEntity newSpec = new ProductSpecificationEntity();
                newSpec.setAttributes(specAttributes);
                newSpec.setProduct(saved);
                specRepo.save(newSpec);
            }
        }

        // Reload complete entity (including spec) for the return value
        entityManager.flush();
        entityManager.clear();
        return productRepo.findById(saved.getId())
                .map(mapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Product not found after save: " + saved.getId()));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productRepo.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findBySlug(String slug) {
        return productRepo.findBySlug(slug)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        productRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAllWithFilters(
            UUID categoryId,
            UUID sellerId,
            String status,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Use custom repository method instead of Specification to benefit from @EntityGraph
        Page<ProductEntity> entityPage = productRepo.findAllWithFilters(
                categoryId, sellerId, status, keyword,
                minPrice, maxPrice, minRating,
                pageable
        );

        return entityPage.map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productRepo.findAllById(ids).stream()
                .filter(entity -> !Boolean.TRUE.equals(entity.getIsDeleted()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findBySellerIdWithFilters(
            UUID sellerId,
            String status,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductEntity> entityPage = productRepo.findBySellerIdWithFilters(
                sellerId, status, keyword, pageable
        );

        return entityPage.map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByCategoryIdAndStatus(UUID categoryId, String status, int limit, UUID excludeProductId) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findRelatedProducts(categoryId, status, excludeProductId, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
