package com.example.sellerservice.repository;

import com.example.sellerservice.entity.SellerDocument;
import com.example.sellerservice.entity.enums.DocumentStatus;
import com.example.sellerservice.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerDocumentRepository extends JpaRepository<SellerDocument, UUID> {

    List<SellerDocument> findAllBySeller_Id(UUID sellerId);

    List<SellerDocument> findAllBySeller_IdAndStatus(UUID sellerId, DocumentStatus status);

    Optional<SellerDocument> findBySeller_IdAndDocumentType(UUID sellerId, DocumentType documentType);

    boolean existsBySeller_IdAndDocumentType(UUID sellerId, DocumentType documentType);

    List<SellerDocument> findAllByStatus(DocumentStatus status);

    long countBySeller_IdAndStatus(UUID sellerId, DocumentStatus status);
}

