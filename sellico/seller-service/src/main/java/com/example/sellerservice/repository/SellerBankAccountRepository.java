package com.example.sellerservice.repository;

import com.example.sellerservice.entity.SellerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, UUID> {

    Optional<SellerBankAccount> findBySeller_Id(UUID sellerId);

    boolean existsBySeller_Id(UUID sellerId);

    boolean existsByAccountNumber(String accountNumber);
}

