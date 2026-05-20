package com.example.sellerservice.dto.response;

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
public class BankAccountResponse {
    private UUID id;
    private String bankName;
    private String bankCode;
    private String branchName;
    private String accountNumber;
    private String accountHolderName;
    private Boolean isVerified;
    private Instant createdAt;
}

