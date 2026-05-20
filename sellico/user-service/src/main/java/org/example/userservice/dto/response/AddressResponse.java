package org.example.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AddressResponse {
    private UUID id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String district;
    private String ward;
    private String addressLine;
    private String fullAddress;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}

