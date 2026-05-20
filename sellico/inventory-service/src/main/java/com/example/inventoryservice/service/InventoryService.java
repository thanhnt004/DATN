package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.request.*;
import com.example.inventoryservice.dto.response.*;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryLog;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.enums.ReservationStatus;
import com.example.inventoryservice.exception.InventoryErrorCode;
import com.example.inventoryservice.exception.InventoryException;
import com.example.inventoryservice.mapper.InventoryMapper;
import com.example.inventoryservice.repository.InventoryLogRepository;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryLogRepository logRepository;
    private final InventoryMapper mapper;

    // =====================================================
    // Inventory CRUD Operations
    // =====================================================

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsBySkuId(request.getSkuId())) {
            throw new InventoryException(InventoryErrorCode.INVENTORY_ALREADY_EXISTS);
        }

        Inventory inventory = Inventory.builder()
                .skuId(request.getSkuId())
                .totalStock(request.getTotalStock())
                .reservedStock(0)
                .lowStockThreshold(request.getLowStockThreshold())
                .locationCode(request.getLocationCode())
                .build();

        inventory = inventoryRepository.save(inventory);

        // Log initial stock if any
        if (request.getTotalStock() > 0) {
            InventoryLog logEntry = InventoryLog.restock(inventory, request.getTotalStock(), null, "Initial stock");
            logRepository.save(logEntry);
        }

        log.info("Created inventory for SKU: {}", request.getSkuId());
        return mapper.toResponse(inventory);
    }

    /**
     * Batch create or update inventories for multiple SKUs.
     * If inventory already exists for a SKU, it updates totalStock/threshold/location.
     * If not, it creates a new record.
     */
    @Transactional
    public List<InventoryResponse> batchUpsertInventory(List<CreateInventoryRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();

        List<UUID> skuIds = requests.stream().map(CreateInventoryRequest::getSkuId).toList();
        Map<UUID, Inventory> existingMap = inventoryRepository.findAllBySkuIds(skuIds).stream()
                .collect(Collectors.toMap(Inventory::getSkuId, i -> i));

        List<InventoryResponse> results = new ArrayList<>();
        for (CreateInventoryRequest req : requests) {
            Inventory existing = existingMap.get(req.getSkuId());
            if (existing != null) {
                // Update existing
                int oldTotal = existing.getTotalStock();
                existing.setTotalStock(req.getTotalStock());
                existing.setLowStockThreshold(req.getLowStockThreshold());
                if (req.getLocationCode() != null) {
                    existing.setLocationCode(req.getLocationCode());
                }
                existing = inventoryRepository.save(existing);
                int diff = req.getTotalStock() - oldTotal;
                if (diff != 0) {
                    InventoryLog logEntry = InventoryLog.adjustment(existing, diff, "Batch upsert stock update");
                    logRepository.save(logEntry);
                }
                results.add(mapper.toResponse(existing));
            } else {
                // Create new
                Inventory inventory = Inventory.builder()
                        .skuId(req.getSkuId())
                        .totalStock(req.getTotalStock())
                        .reservedStock(0)
                        .lowStockThreshold(req.getLowStockThreshold())
                        .locationCode(req.getLocationCode())
                        .build();
                inventory = inventoryRepository.save(inventory);
                if (req.getTotalStock() > 0) {
                    InventoryLog logEntry = InventoryLog.restock(inventory, req.getTotalStock(), null, "Initial stock");
                    logRepository.save(logEntry);
                }
                results.add(mapper.toResponse(inventory));
            }
        }
        log.info("Batch upsert inventory for {} SKUs", requests.size());
        return results;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID skuId) {
        Inventory inventory = inventoryRepository.findById(skuId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));
        return mapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventories(List<UUID> skuIds) {
        List<Inventory> inventories = inventoryRepository.findAllBySkuIds(skuIds);
        return mapper.toResponseList(inventories);
    }

    @Transactional
    public InventoryResponse updateInventory(UUID skuId, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));

        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getLocationCode() != null) {
            inventory.setLocationCode(request.getLocationCode());
        }

        inventory = inventoryRepository.save(inventory);
        return mapper.toResponse(inventory);
    }

    // =====================================================
    // Stock Operations
    // =====================================================

    @Transactional
    public InventoryResponse restock(RestockRequest request) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(request.getSkuId())
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));

        inventory.addStock(request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        // Create log entry
        InventoryLog logEntry = InventoryLog.restock(inventory, request.getQuantity(),
                request.getReferenceId(), request.getNote());
        logRepository.save(logEntry);

        log.info("Restocked SKU {}: +{} units", request.getSkuId(), request.getQuantity());
        return mapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse adjustStock(AdjustStockRequest request) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(request.getSkuId())
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));

        int newTotalStock = inventory.getTotalStock() + request.getChangeAmount();
        if (newTotalStock < 0) {
            throw new InventoryException(InventoryErrorCode.INVALID_STOCK_QUANTITY,
                    "Adjustment would result in negative stock");
        }
        if (newTotalStock < inventory.getReservedStock()) {
            throw new InventoryException(InventoryErrorCode.INVALID_STOCK_QUANTITY,
                    "Adjustment would result in total stock less than reserved stock");
        }

        inventory.setTotalStock(newTotalStock);
        inventory = inventoryRepository.save(inventory);

        // Create log entry
        InventoryLog logEntry = InventoryLog.adjustment(inventory, request.getChangeAmount(), request.getNote());
        logRepository.save(logEntry);

        log.info("Adjusted SKU {} stock by {} units", request.getSkuId(), request.getChangeAmount());
        return mapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<StockAvailabilityResponse> checkAvailability(List<UUID> skuIds) {
        List<Inventory> inventories = inventoryRepository.findAllBySkuIds(skuIds);

        Map<UUID, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getSkuId, i -> i));

        return skuIds.stream()
                .map(skuId -> {
                    Inventory inventory = inventoryMap.get(skuId);
                    if (inventory == null) {
                        return StockAvailabilityResponse.builder()
                                .skuId(skuId)
                                .availableStock(0)
                                .isAvailable(false)
                                .isLowStock(false)
                                .build();
                    }
                    return mapper.toAvailabilityResponse(inventory);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventories() {
        List<Inventory> inventories = inventoryRepository.findLowStockInventories();
        return mapper.toResponseList(inventories);
    }

    // =====================================================
    // Reservation Operations
    // =====================================================

    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        List<UUID> skuIds = request.getItems().stream()
                .map(ReserveStockRequest.ReservationItem::getSkuId)
                .toList();

        // Get all inventories with lock
        List<Inventory> inventories = inventoryRepository.findAllBySkuIdsWithLock(skuIds);
        Map<UUID, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getSkuId, i -> i));

        List<ReservationResponse> successfulReservations = new ArrayList<>();
        List<ReserveStockResponse.FailedReservation> failedItems = new ArrayList<>();

        Instant expiresAt = Instant.now().plus(request.getDurationMinutes(), ChronoUnit.MINUTES);

        for (ReserveStockRequest.ReservationItem item : request.getItems()) {
            Inventory inventory = inventoryMap.get(item.getSkuId());

            if (inventory == null) {
                failedItems.add(ReserveStockResponse.FailedReservation.builder()
                        .skuId(item.getSkuId())
                        .requestedQuantity(item.getQuantity())
                        .availableQuantity(0)
                        .reason("Inventory not found")
                        .build());
                continue;
            }

            // Check if reservation already exists
            if (reservationRepository.existsByOrderIdAndInventory_SkuId(request.getOrderId(), item.getSkuId())) {
                failedItems.add(ReserveStockResponse.FailedReservation.builder()
                        .skuId(item.getSkuId())
                        .requestedQuantity(item.getQuantity())
                        .availableQuantity(inventory.getAvailableStock())
                        .reason("Reservation already exists for this order")
                        .build());
                continue;
            }

            if (!inventory.canReserve(item.getQuantity())) {
                failedItems.add(ReserveStockResponse.FailedReservation.builder()
                        .skuId(item.getSkuId())
                        .requestedQuantity(item.getQuantity())
                        .availableQuantity(inventory.getAvailableStock())
                        .reason("Insufficient stock")
                        .build());
                continue;
            }

            // Reserve stock
            inventory.reserve(item.getQuantity());
            inventoryRepository.save(inventory);

            // Create reservation
            InventoryReservation reservation = InventoryReservation.builder()
                    .inventory(inventory)
                    .orderId(request.getOrderId())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.PENDING)
                    .expiresAt(expiresAt)
                    .build();
            reservation = reservationRepository.save(reservation);

            successfulReservations.add(mapper.toReservationResponse(reservation));
        }

        boolean allSuccess = failedItems.isEmpty();

        // If any failed, rollback successful ones
        if (!allSuccess && !successfulReservations.isEmpty()) {
            for (ReservationResponse res : successfulReservations) {
                cancelReservationInternal(res.getId());
            }
            successfulReservations.clear();
        }

        log.info("Reserve stock for order {}: success={}, failed={}",
                request.getOrderId(), successfulReservations.size(), failedItems.size());

        return ReserveStockResponse.builder()
                .orderId(request.getOrderId())
                .success(allSuccess)
                .reservations(allSuccess ? successfulReservations : List.of())
                .failedItems(failedItems)
                .build();
    }

    @Transactional
    public List<ReservationResponse> confirmReservation(UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new InventoryException(InventoryErrorCode.RESERVATION_NOT_FOUND);
        }

        List<ReservationResponse> confirmedReservations = new ArrayList<>();

        for (InventoryReservation reservation : reservations) {
            if (!reservation.canConfirm()) {
                if (reservation.isExpired()) {
                    throw new InventoryException(InventoryErrorCode.RESERVATION_EXPIRED);
                }
                throw new InventoryException(InventoryErrorCode.CANNOT_CONFIRM_RESERVATION);
            }

            Inventory inventory = inventoryRepository.findBySkuIdWithLock(reservation.getInventory().getSkuId())
                    .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));

            // Confirm reservation
            reservation.confirm();
            inventory.confirmReservation(reservation.getQuantity());

            inventoryRepository.save(inventory);
            reservationRepository.save(reservation);

            // Create sale log
            InventoryLog logEntry = InventoryLog.sale(inventory, reservation.getQuantity(),
                    orderId, "Order confirmed");
            logRepository.save(logEntry);

            confirmedReservations.add(mapper.toReservationResponse(reservation));
        }

        log.info("Confirmed reservations for order {}: {} items", orderId, confirmedReservations.size());
        return confirmedReservations;
    }

    @Transactional
    public List<ReservationResponse> cancelReservation(UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new InventoryException(InventoryErrorCode.RESERVATION_NOT_FOUND);
        }

        List<ReservationResponse> cancelledReservations = new ArrayList<>();

        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                continue; // Skip already confirmed
            }

            if (reservation.canCancel()) {
                Inventory inventory = inventoryRepository.findBySkuIdWithLock(reservation.getInventory().getSkuId())
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));

                reservation.cancel();
                inventory.releaseReservation(reservation.getQuantity());

                inventoryRepository.save(inventory);
                reservationRepository.save(reservation);

                cancelledReservations.add(mapper.toReservationResponse(reservation));
            }
        }

        log.info("Cancelled reservations for order {}: {} items", orderId, cancelledReservations.size());
        return cancelledReservations;
    }

    private void cancelReservationInternal(UUID reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElse(null);

        if (reservation != null && reservation.canCancel()) {
            Inventory inventory = reservation.getInventory();
            reservation.cancel();
            inventory.releaseReservation(reservation.getQuantity());
            inventoryRepository.save(inventory);
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public int processExpiredReservations() {
        List<InventoryReservation> expiredReservations = reservationRepository.findExpiredReservations(Instant.now());

        int count = 0;
        for (InventoryReservation reservation : expiredReservations) {
            Inventory inventory = inventoryRepository.findBySkuIdWithLock(reservation.getInventory().getSkuId())
                    .orElse(null);

            if (inventory != null) {
                reservation.markExpired();
                inventory.releaseReservation(reservation.getQuantity());

                inventoryRepository.save(inventory);
                reservationRepository.save(reservation);

                // Create log entry
                InventoryLog logEntry = InventoryLog.expiredReservation(inventory, reservation.getQuantity(),
                        reservation.getId(), "Reservation expired");
                logRepository.save(logEntry);

                count++;
            }
        }

        if (count > 0) {
            log.info("Processed {} expired reservations", count);
        }

        return count;
    }

    // =====================================================
    // Log Operations
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getInventoryLogs(UUID skuId) {
        List<InventoryLog> logs = logRepository.findAllByInventory_SkuIdOrderByCreatedAtDesc(skuId);
        return mapper.toLogResponseList(logs);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations(UUID skuId) {
        List<InventoryReservation> reservations = reservationRepository.findAllByInventory_SkuId(skuId);
        return mapper.toReservationResponseList(reservations);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getOrderReservations(UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);
        return mapper.toReservationResponseList(reservations);
    }
}

