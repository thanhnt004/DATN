package com.example.orderservice.adapter.web;

import com.example.orderservice.application.dto.response.AdminOrderStatsResponse;
import com.example.orderservice.application.dto.response.DetailedReportSummaryResponse;
import com.example.orderservice.application.dto.response.DetailedReportTrendResponse;
import com.example.orderservice.application.dto.response.DetailedReportCategoryResponse;
import com.example.orderservice.application.dto.response.DetailedReportProductSalesResponse;
import com.example.orderservice.application.dto.response.OrderPageResponse;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.dto.response.RevenueReportResponse;
import com.example.orderservice.application.exception.UnauthorizedException;
import com.example.orderservice.application.port.input.QueryOrderUseCase;
import com.example.orderservice.application.port.input.SellerReportUseCase;
import com.example.orderservice.application.port.input.UpdateOrderUseCase;
import com.example.orderservice.application.dto.command.ConfirmOrderCommand;
import com.example.orderservice.application.dto.command.ShipOrderCommand;
import com.example.orderservice.adapter.web.dto.ConfirmOrderRequestDto;
import com.example.orderservice.adapter.web.dto.ShipOrderRequestDto;
import com.example.orderservice.domain.model.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import response.ApiResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/orders")
@RequiredArgsConstructor
@Slf4j
public class SellerOrderController {

    private final SellerReportUseCase sellerReportUseCase;
    private final QueryOrderUseCase queryOrderUseCase;
    private final UpdateOrderUseCase updateOrderUseCase;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<OrderPageResponse<OrderResponse>>> getOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        String sellerId = jwt.getSubject();
        log.debug("Seller orders request: sellerId={}, status={}, orderNumber={}, startDate={}, endDate={}, page={}, size={}",
                sellerId, status, orderNumber, startDate, endDate, page, size);
        OrderPageResponse<OrderResponse> response = queryOrderUseCase.getSellerOrders(
                UUID.fromString(sellerId), status, orderNumber, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String sellerId = jwt.getSubject();
        OrderResponse response = queryOrderUseCase.getOrderById(orderId);
        
        // Verify that the order belongs to the seller
        if (!response.getSellerId().equals(UUID.fromString(sellerId))) {
            throw new UnauthorizedException("Order does not belong to this seller");
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/overview")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<AdminOrderStatsResponse>> getOverallStats(@AuthenticationPrincipal Jwt jwt) {
        String sellerId =  jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.success(sellerReportUseCase.getOverallStats(UUID.fromString(sellerId))));
    }

    @GetMapping("/stats/revenue")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "period", defaultValue = "DAILY") String period
    ) {
        String sellerId =  jwt.getSubject();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        return ResponseEntity.ok(ApiResponse.success(sellerReportUseCase.getRevenueReport(UUID.fromString(sellerId), startInstant, endInstant, period)));
    }

    @GetMapping("/stats/detailed/summary")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<DetailedReportSummaryResponse>> getDetailedReportSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "period", defaultValue = "DAILY") String period
    ) {
        String sellerId = jwt.getSubject();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        DetailedReportSummaryResponse response = sellerReportUseCase.getDetailedReportSummary(UUID.fromString(sellerId), startInstant, endInstant, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/detailed/trend")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<DetailedReportTrendResponse>> getDetailedReportTrend(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "period", defaultValue = "DAILY") String period
    ) {
        String sellerId = jwt.getSubject();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        DetailedReportTrendResponse response = sellerReportUseCase.getDetailedReportTrend(UUID.fromString(sellerId), startInstant, endInstant, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/detailed/category")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<DetailedReportCategoryResponse>> getDetailedReportCategory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "period", defaultValue = "DAILY") String period
    ) {
        String sellerId = jwt.getSubject();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        DetailedReportCategoryResponse response = sellerReportUseCase.getDetailedReportCategory(UUID.fromString(sellerId), startInstant, endInstant, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/detailed/products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<DetailedReportProductSalesResponse>> getDetailedReportProducts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "period", defaultValue = "DAILY") String period,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sortBy", defaultValue = "revenue") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir
    ) {
        String sellerId = jwt.getSubject();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        DetailedReportProductSalesResponse response = sellerReportUseCase.getDetailedReportProducts(
                UUID.fromString(sellerId), startInstant, endInstant, period,
                page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════ ORDER UPDATE ENDPOINTS ═══════════

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "note", required = false) String note
    ) {
        String sellerId = jwt.getSubject();
        ConfirmOrderCommand command = ConfirmOrderCommand.builder()
                .orderId(orderId)
                .sellerId(UUID.fromString(sellerId))
                .note(note)
                .build();
        OrderResponse response = updateOrderUseCase.confirmOrder(command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ShipOrderRequestDto request
    ) {
        String sellerId = jwt.getSubject();
        ShipOrderCommand command = ShipOrderCommand.builder()
                .orderId(orderId)
                .sellerId(UUID.fromString(sellerId))
                .shippingProvider(request.getShippingProvider())
                .trackingNumber(request.getTrackingNumber())
                .note(request.getNote())
                .build();
        OrderResponse response = updateOrderUseCase.shipOrder(command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/deliver")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String sellerId = jwt.getSubject();
        OrderResponse order = queryOrderUseCase.getOrderById(orderId);
        if (!order.getSellerId().equals(UUID.fromString(sellerId))) {
            throw new UnauthorizedException("Order does not belong to this seller");
        }
        OrderResponse response = updateOrderUseCase.deliverOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/complete")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String sellerId = jwt.getSubject();
        OrderResponse response = updateOrderUseCase.completeOrder(orderId, UUID.fromString(sellerId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
