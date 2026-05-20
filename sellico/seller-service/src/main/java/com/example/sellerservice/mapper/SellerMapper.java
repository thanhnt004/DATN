package com.example.sellerservice.mapper;

import com.example.sellerservice.dto.response.*;
import com.example.sellerservice.entity.Seller;
import com.example.sellerservice.entity.SellerBankAccount;
import com.example.sellerservice.entity.SellerDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SellerMapper {

    SellerResponse toResponse(Seller seller);

    List<SellerResponse> toResponseList(List<Seller> sellers);

    @Mapping(target = "isFollowing", ignore = true)
    SellerSummaryResponse toSummaryResponse(Seller seller);

    List<SellerSummaryResponse> toSummaryResponseList(List<Seller> sellers);

    SellerDocumentResponse toDocumentResponse(SellerDocument document);

    List<SellerDocumentResponse> toDocumentResponseList(List<SellerDocument> documents);

    BankAccountResponse toBankAccountResponse(SellerBankAccount bankAccount);

    @Mapping(target = "sellerId", source = "id")
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "pendingOrders", ignore = true)
    @Mapping(target = "processingOrders", ignore = true)
    @Mapping(target = "lowStockProducts", ignore = true)
    SellerDashboardResponse toDashboardResponse(Seller seller);
}

