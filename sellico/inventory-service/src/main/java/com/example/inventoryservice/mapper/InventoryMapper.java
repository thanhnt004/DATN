package com.example.inventoryservice.mapper;

import com.example.inventoryservice.dto.response.InventoryLogResponse;
import com.example.inventoryservice.dto.response.InventoryResponse;
import com.example.inventoryservice.dto.response.ReservationResponse;
import com.example.inventoryservice.dto.response.StockAvailabilityResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryLog;
import com.example.inventoryservice.entity.InventoryReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "availableStock", expression = "java(inventory.getAvailableStock())")
    @Mapping(target = "isLowStock", expression = "java(inventory.isLowStock())")
    InventoryResponse toResponse(Inventory inventory);

    List<InventoryResponse> toResponseList(List<Inventory> inventories);

    @Mapping(target = "skuId", source = "inventory.skuId")
    ReservationResponse toReservationResponse(InventoryReservation reservation);

    List<ReservationResponse> toReservationResponseList(List<InventoryReservation> reservations);

    @Mapping(target = "skuId", source = "inventory.skuId")
    InventoryLogResponse toLogResponse(InventoryLog log);

    List<InventoryLogResponse> toLogResponseList(List<InventoryLog> logs);

    default StockAvailabilityResponse toAvailabilityResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        return StockAvailabilityResponse.builder()
                .skuId(inventory.getSkuId())
                .availableStock(inventory.getAvailableStock())
                .isAvailable(inventory.getAvailableStock() > 0)
                .isLowStock(inventory.isLowStock())
                .build();
    }

    List<StockAvailabilityResponse> toAvailabilityResponseList(List<Inventory> inventories);
}

