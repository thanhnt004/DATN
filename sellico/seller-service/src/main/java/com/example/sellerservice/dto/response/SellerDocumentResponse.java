package com.example.sellerservice.dto.response;

import com.example.sellerservice.entity.enums.DocumentStatus;
import com.example.sellerservice.entity.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDocumentResponse {
    private UUID id;
    private DocumentType documentType;
    private String documentUrl;
    private String documentNumber;
    private DocumentStatus status;
    private String rejectionReason;
    private Instant verifiedAt;
    private Instant createdAt;
}

