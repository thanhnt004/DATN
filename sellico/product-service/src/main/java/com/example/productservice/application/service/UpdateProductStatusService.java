package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductStatusCommand;
import com.example.productservice.application.port.in.UpdateProductStatusUseCase;
import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateProductStatusService implements UpdateProductStatusUseCase {

    private static final Set<String> VALID_STATUSES = Set.of(
            "DRAFT", "PENDING", "ACTIVE", "BANNED", "DELETED"
    );

    private final ProductRepositoryPort productRepositoryPort;
    private final ProductEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public Product updateStatus(UpdateProductStatusCommand command) {
        // Validate status
        if (!VALID_STATUSES.contains(command.getStatus())) {
            throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS);
        }

        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // Check ownership if sellerId is provided (seller updating their own product)
        if (command.getSellerId() != null && !product.getSellerId().equals(command.getSellerId())) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // Validate status transitions
        validateStatusTransition(product.getStatus(), command.getStatus());

        // Update status
        product.setStatus(command.getStatus());
        product.setUpdatedAt(Instant.now());

        // If status is DELETED, also set isDeleted flag
        if ("DELETED".equals(command.getStatus())) {
            product.setIsDeleted(true);
        }

        Product saved = productRepositoryPort.updateProductOnly(product);
        eventPublisher.publishProductStatusChanged(saved);
        return saved;
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Define valid transitions
        // DRAFT -> PENDING (seller submits for review)
        // PENDING -> ACTIVE (admin approves) or BANNED (admin rejects)
        // ACTIVE -> BANNED (admin bans) or DELETED (seller deletes)
        // BANNED -> ACTIVE (admin unbans)
        // Any -> DELETED (soft delete)

        if ("DELETED".equals(currentStatus)) {
            throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Allow transition to DELETED from any non-deleted status
        if ("DELETED".equals(newStatus)) {
            return;
        }

        // Validate specific transitions
        switch (currentStatus) {
            case "DRAFT":
                if (!"PENDING".equals(newStatus) && !"ACTIVE".equals(newStatus)) {
                    throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS_TRANSITION);
                }
                break;
            case "PENDING":
                if (!"ACTIVE".equals(newStatus) && !"BANNED".equals(newStatus) && !"DRAFT".equals(newStatus)) {
                    throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS_TRANSITION);
                }
                break;
            case "ACTIVE":
                if (!"BANNED".equals(newStatus) && !"DRAFT".equals(newStatus)) {
                    throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS_TRANSITION);
                }
                break;
            case "BANNED":
                if (!"ACTIVE".equals(newStatus)) {
                    throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS_TRANSITION);
                }
                break;
        }
    }
}

