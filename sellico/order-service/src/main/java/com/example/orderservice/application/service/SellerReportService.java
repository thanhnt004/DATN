package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.response.AdminOrderStatsResponse;
import com.example.orderservice.application.dto.response.DetailedReportSummaryResponse;
import com.example.orderservice.application.dto.response.DetailedReportTrendResponse;
import com.example.orderservice.application.dto.response.DetailedReportCategoryResponse;
import com.example.orderservice.application.dto.response.DetailedReportProductSalesResponse;
import com.example.orderservice.application.dto.response.OrderPageResponse;
import com.example.orderservice.application.dto.response.RevenueReportResponse;
import com.example.orderservice.application.port.input.SellerReportUseCase;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.domain.port.output.OrderRepository;
import com.example.orderservice.infrastructure.client.CategoryFeignClient;
import com.example.orderservice.infrastructure.client.ProductFeignClient;
import com.example.orderservice.infrastructure.persistence.entity.PlatformFeeConfigJpaEntity;
import com.example.orderservice.infrastructure.persistence.repository.PlatformFeeConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import response.ApiResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerReportService implements SellerReportUseCase {

    private final OrderRepository orderRepository;
    private final PlatformFeeConfigJpaRepository platformFeeConfigRepository;
    private final ProductFeignClient productFeignClient;
    private final CategoryFeignClient categoryFeignClient;

    // Default rates if not configured
    private static final BigDecimal DEFAULT_PAYMENT_FEE_RATE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal DEFAULT_COMMISSION_FEE_RATE = new BigDecimal("0.03"); // 3%
    private static final BigDecimal DEFAULT_SERVICE_FEE_RATE = new BigDecimal("0.01"); // 1%

    @Override
    public AdminOrderStatsResponse getOverallStats(UUID sellerId) {
        Map<OrderStatus, Long> ordersByStatus = orderRepository.countSellerOrdersByStatus(sellerId);
        Map<OrderStatus, BigDecimal> revenueByStatus = orderRepository.sumSellerRevenueByStatus(sellerId);
        BigDecimal totalRevenue = orderRepository.sumSellerCompletedRevenue(sellerId);

        long totalOrders = ordersByStatus.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Long> ordersByStatusStr = ordersByStatus.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));

        Map<String, BigDecimal> revenueByStatusStr = revenueByStatus.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));

        return AdminOrderStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .pendingOrders(ordersByStatus.getOrDefault(OrderStatus.PENDING, 0L))
                .confirmedOrders(ordersByStatus.getOrDefault(OrderStatus.CONFIRMED, 0L))
                .shippedOrders(ordersByStatus.getOrDefault(OrderStatus.SHIPPED, 0L))
                .deliveredOrders(ordersByStatus.getOrDefault(OrderStatus.DELIVERED, 0L))
                .completedOrders(ordersByStatus.getOrDefault(OrderStatus.COMPLETED, 0L))
                .cancelledOrders(ordersByStatus.getOrDefault(OrderStatus.CANCELLED, 0L))
                .ordersByStatus(ordersByStatusStr)
                .revenueByStatus(revenueByStatusStr)
                .build();
    }

    @Override
    public RevenueReportResponse getRevenueReport(UUID sellerId, Instant startDate, Instant endDate, String periodType) {
        List<Object[]> reportData = orderRepository.getSellerDailyRevenueReport(sellerId, startDate, endDate);
        Map<String, RevenueReportResponse.RevenueDataPoint> aggregated = new java.util.LinkedHashMap<>();

        for (Object[] row : reportData) {
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof java.util.Date) {
                date = ((java.util.Date) row[0]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            long count = (Long) row[2];
            String period = formatPeriod(date, periodType);

            aggregated.compute(period, (key, existing) -> {
                if (existing == null) {
                    return RevenueReportResponse.RevenueDataPoint.builder()
                            .period(period)
                            .revenue(revenue)
                            .orderCount(count)
                            .build();
                }
                existing.setRevenue(existing.getRevenue().add(revenue));
                existing.setOrderCount(existing.getOrderCount() + count);
                return existing;
            });
        }

        List<RevenueReportResponse.RevenueDataPoint> dataPoints = aggregated.values().stream().toList();

        BigDecimal totalRevenueInRange = dataPoints.stream()
                .map(RevenueReportResponse.RevenueDataPoint::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrdersInRange = dataPoints.stream()
                .mapToLong(RevenueReportResponse.RevenueDataPoint::getOrderCount)
                .sum();

        return RevenueReportResponse.builder()
                .dataPoints(dataPoints)
                .totalRevenueInRange(totalRevenueInRange)
                .totalOrdersInRange(totalOrdersInRange)
                .build();
    }

    @Override
    public DetailedReportSummaryResponse getDetailedReportSummary(UUID sellerId, Instant startDate, Instant endDate, String periodType) {
        List<Order> orders = orderRepository.findBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate);

        // 1. Overview
        BigDecimal totalGrossSales = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        long totalOrderCount = orders.size();
        long cancelledCount = 0;
        long completedCount = 0;

        // Waterfall Chart aggregations
        BigDecimal baseProductRevenue = BigDecimal.ZERO;
        BigDecimal shopVouchers = BigDecimal.ZERO;
        // shippingSubsidies = số tiền seller TỰ chịu cho phí ship.
        // Hiện tại Order chỉ lưu shippingFee = buyer trả -> pass-through cho courier, không phải tiền seller chịu.
        // Để 0 cho đến khi có field seller_shipping_subsidy.
        BigDecimal shippingSubsidies = BigDecimal.ZERO;

        // Fees
        BigDecimal paymentFees = BigDecimal.ZERO;
        BigDecimal commissionFees = BigDecimal.ZERO;
        BigDecimal serviceFees = BigDecimal.ZERO;

        // Get latest config
        PlatformFeeConfigJpaEntity config = platformFeeConfigRepository.findLatestConfig()
                .orElse(null);
        BigDecimal paymentRate = config != null ? config.getPaymentFeeRate() : DEFAULT_PAYMENT_FEE_RATE;
        BigDecimal commissionRate = config != null ? config.getCommissionFeeRate() : DEFAULT_COMMISSION_FEE_RATE;
        BigDecimal serviceRate = config != null ? config.getServiceFeeRate() : DEFAULT_SERVICE_FEE_RATE;

        for (Order order : orders) {
            BigDecimal orderTotal = order.getTotalAmount().amount();
            totalGrossSales = totalGrossSales.add(orderTotal);

            if (order.getStatus() == OrderStatus.CANCELLED) {
                cancelledCount++;
            } else if (order.getStatus() == OrderStatus.COMPLETED) {
                completedCount++;
                netRevenue = netRevenue.add(orderTotal);

                // Waterfall calculations
                baseProductRevenue = baseProductRevenue.add(order.getSubtotal().amount());
                shopVouchers = shopVouchers.add(order.getDiscountAmount().amount());
                // shippingSubsidies giữ 0 - xem comment phía trên về sellerShipCost

                // Phí sàn tính trên giá trị seller thực sự nhận (subtotal - discount), không tính trên shippingFee
                BigDecimal feeBase = order.getSubtotal().amount().subtract(order.getDiscountAmount().amount());
                paymentFees = paymentFees.add(feeBase.multiply(paymentRate));
                commissionFees = commissionFees.add(feeBase.multiply(commissionRate));
                serviceFees = serviceFees.add(feeBase.multiply(serviceRate));
            }
        }

        double cancellationRate = totalOrderCount > 0 ? (double) cancelledCount / totalOrderCount * 100 : 0;
        double returnRate = 0; // Mock for now
        BigDecimal aov = completedCount > 0 ? netRevenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        paymentFees = paymentFees.setScale(2, RoundingMode.HALF_UP);
        commissionFees = commissionFees.setScale(2, RoundingMode.HALF_UP);
        serviceFees = serviceFees.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPlatformFees = paymentFees.add(commissionFees).add(serviceFees);
        // payout = doanh thu sản phẩm - voucher shop chịu - phí ship seller chịu - phí sàn
        BigDecimal payout = baseProductRevenue
                .subtract(shopVouchers)
                .subtract(shippingSubsidies)
                .subtract(totalPlatformFees)
                .setScale(2, RoundingMode.HALF_UP);

        return DetailedReportSummaryResponse.builder()
                .totalGrossSales(totalGrossSales)
                .netRevenue(netRevenue)
                .totalOrderCount(totalOrderCount)
                .cancellationRate(cancellationRate)
                .returnRate(returnRate)
                .averageOrderValue(aov)
                .waterfallChart(DetailedReportSummaryResponse.WaterfallChartData.builder()
                        .baseProductRevenue(baseProductRevenue)
                        .shopVouchers(shopVouchers)
                        .shippingSubsidies(shippingSubsidies)
                        .platformFees(totalPlatformFees)
                        .paymentFees(paymentFees)
                        .commissionFees(commissionFees)
                        .serviceFees(serviceFees)
                        .payout(payout)
                        .build())
                .build();
    }

    @Override
    public DetailedReportTrendResponse getDetailedReportTrend(UUID sellerId, Instant startDate, Instant endDate, String periodType) {
        LocalDate startLocalDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endLocalDate = endDate.atZone(ZoneId.systemDefault()).toLocalDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, endLocalDate) + 1;

        Instant previousStartDate = startLocalDate.minusDays(daysBetween).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant previousEndDate = endLocalDate.minusDays(daysBetween).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        List<Order> currentOrders = orderRepository.findBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate);
        List<Order> previousOrders = orderRepository.findBySellerIdAndCreatedAtBetween(sellerId, previousStartDate, previousEndDate);

        return DetailedReportTrendResponse.builder()
                .revenueByTime(aggregateRevenueByPeriod(currentOrders, periodType))
                .previousRevenueByTime(aggregateRevenueByPeriod(previousOrders, periodType))
                .build();
    }

    private List<DetailedReportTrendResponse.RevenueDataPoint> aggregateRevenueByPeriod(List<Order> orders, String periodType) {
        Map<String, DetailedReportTrendResponse.RevenueDataPoint> aggregated = new HashMap<>();
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.COMPLETED) continue;
            LocalDate date = order.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            String period = formatPeriod(date, periodType);
            BigDecimal amount = order.getTotalAmount().amount();
            aggregated.compute(period, (k, v) -> {
                if (v == null) {
                    return DetailedReportTrendResponse.RevenueDataPoint.builder()
                            .period(period)
                            .revenue(amount)
                            .orderCount(1L)
                            .build();
                }
                v.setRevenue(v.getRevenue().add(amount));
                v.setOrderCount(v.getOrderCount() + 1);
                return v;
            });
        }
        return aggregated.values().stream()
                .sorted(Comparator.comparing(DetailedReportTrendResponse.RevenueDataPoint::getPeriod))
                .collect(Collectors.toList());
    }

    @Override
    public DetailedReportCategoryResponse getDetailedReportCategory(UUID sellerId, Instant startDate, Instant endDate, String periodType) {
        List<Order> orders = orderRepository.findBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate);

        List<Order> completedOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED && o.getItems() != null && !o.getItems().isEmpty())
                .toList();

        if (completedOrders.isEmpty()) {
            return DetailedReportCategoryResponse.builder()
                    .revenueByCategory(new ArrayList<>())
                    .build();
        }

        Set<UUID> productIds = completedOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(com.example.orderservice.domain.model.OrderItem::getProductId)
                .collect(Collectors.toSet());

        Map<UUID, UUID> productToCategoryMap = fetchProductToCategoryMap(productIds);

        final UUID UNKNOWN_CATEGORY_ID = new UUID(0L, 0L);

        Map<UUID, BigDecimal> categoryRevenueMap = new HashMap<>();
        Map<UUID, Long> categoryQuantityMap = new HashMap<>();
        Map<UUID, Set<UUID>> categoryOrderIdsMap = new HashMap<>();

        for (Order order : completedOrders) {
            UUID orderId = order.getId().value();
            for (com.example.orderservice.domain.model.OrderItem item : order.getItems()) {
                UUID cId = productToCategoryMap.getOrDefault(item.getProductId(), UNKNOWN_CATEGORY_ID);
                categoryRevenueMap.merge(cId, item.getSubtotal().amount(), BigDecimal::add);
                categoryQuantityMap.merge(cId, (long) item.getQuantity(), Long::sum);
                categoryOrderIdsMap.computeIfAbsent(cId, k -> new HashSet<>()).add(orderId);
            }
        }

        Map<UUID, String> categoryNameMap = fetchCategoryNames(
                categoryRevenueMap.keySet().stream()
                        .filter(id -> !id.equals(UNKNOWN_CATEGORY_ID))
                        .toList()
        );

        List<UUID> sortedCategoryIds = new ArrayList<>(categoryRevenueMap.keySet());
        sortedCategoryIds.sort((a, b) -> categoryRevenueMap.get(b).compareTo(categoryRevenueMap.get(a)));

        BigDecimal totalCategoryRev = categoryRevenueMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DetailedReportCategoryResponse.CategoryRevenueData> revenueByCategory = new ArrayList<>();
        BigDecimal othersRev = BigDecimal.ZERO;
        long othersQuantity = 0;
        Set<UUID> othersOrderIds = new HashSet<>();

        for (int i = 0; i < sortedCategoryIds.size(); i++) {
            UUID cId = sortedCategoryIds.get(i);
            BigDecimal rev = categoryRevenueMap.get(cId);
            Set<UUID> orderIds = categoryOrderIdsMap.getOrDefault(cId, Collections.emptySet());
            long quantity = categoryQuantityMap.getOrDefault(cId, 0L);

            if (i < 3) {
                String name = cId.equals(UNKNOWN_CATEGORY_ID)
                        ? "Danh mục sản phẩm khác"
                        : categoryNameMap.getOrDefault(cId, "Danh mục sản phẩm khác");
                revenueByCategory.add(DetailedReportCategoryResponse.CategoryRevenueData.builder()
                        .categoryName(name)
                        .revenue(rev)
                        .orderCount(orderIds.size())
                        .quantitySold(quantity)
                        .build());
            } else {
                othersRev = othersRev.add(rev);
                othersQuantity += quantity;
                othersOrderIds.addAll(orderIds);
            }
        }

        if (othersRev.compareTo(BigDecimal.ZERO) > 0) {
            revenueByCategory.add(DetailedReportCategoryResponse.CategoryRevenueData.builder()
                    .categoryName("Khác")
                    .revenue(othersRev)
                    .orderCount(othersOrderIds.size())
                    .quantitySold(othersQuantity)
                    .build());
        }

        if (totalCategoryRev.compareTo(BigDecimal.ZERO) > 0) {
            for (var data : revenueByCategory) {
                double percentage = data.getRevenue().divide(totalCategoryRev, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                data.setPercentage(percentage);
            }
        }

        return DetailedReportCategoryResponse.builder()
                .revenueByCategory(revenueByCategory)
                .build();
    }

    private Map<UUID, UUID> fetchProductToCategoryMap(Set<UUID> productIds) {
        if (productIds.isEmpty()) return Collections.emptyMap();
        try {
            log.debug("Fetching product to category map for productIds={}", productIds);
            ApiResponse<List<ProductFeignClient.ProductSummaryResponse>> response = productFeignClient.getBatchProducts(new ArrayList<>(productIds));
            List<ProductFeignClient.ProductSummaryResponse> productSummaries = response != null ? response.getData() : null;
            if (productSummaries == null) {
                log.warn("Product batch response was null for productIds={}", productIds);
                return Collections.emptyMap();
            }
            Map<UUID, UUID> map = new HashMap<>();
            for (var p : productSummaries) {
                if (p.getCategoryId() != null) {
                    map.put(p.getId(), p.getCategoryId());
                } else {
                    log.debug("Product {} has null categoryId", p.getId());
                }
            }
            log.debug("Fetched product to category map: {}", map);
            return map;
        } catch (Exception e) {
            log.error("Failed to fetch product summaries for category mapping: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private Map<UUID, String> fetchCategoryNames(List<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return Collections.emptyMap();
        try {
            log.debug("Fetching category names for ids={} ", categoryIds);
            List<CategoryFeignClient.CategoryResponse> categories = categoryFeignClient.getBatchCategoriesByIds(categoryIds);
            if (categories == null) {
                log.warn("Category batch response was null for ids={}", categoryIds);
                return Collections.emptyMap();
            }
            Map<UUID, String> map = new HashMap<>();
            for (var c : categories) {
                map.put(c.getId(), c.getName());
            }
            log.debug("Fetched {} categories", map.size());
            return map;
        } catch (Exception e) {
            log.error("Failed to fetch category names for ids={}: {}", categoryIds, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    @Override
    public DetailedReportProductSalesResponse getDetailedReportProducts(UUID sellerId, Instant startDate, Instant endDate, String periodType,
                                                                        int page, int size, String sortBy, String sortDir) {
        List<Order> orders = orderRepository.findBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate);

        // Aggregate product sales
        Map<UUID, DetailedReportProductSalesResponse.ProductSalesData> productSalesMap = new HashMap<>();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.COMPLETED && order.getItems() != null) {
                for (com.example.orderservice.domain.model.OrderItem item : order.getItems()) {
                    productSalesMap.compute(item.getProductId(), (k, v) -> {
                        if (v == null) {
                            return DetailedReportProductSalesResponse.ProductSalesData.builder()
                                    .productId(item.getProductId().toString())
                                    .productName(item.getProductName())
                                    .imageUrl(item.getImageUrl())
                                    .totalQuantitySold(item.getQuantity())
                                    .totalRevenue(item.getSubtotal().amount())
                                    .build();
                        }
                        v.setTotalQuantitySold(v.getTotalQuantitySold() + item.getQuantity());
                        v.setTotalRevenue(v.getTotalRevenue().add(item.getSubtotal().amount()));
                        return v;
                    });
                }
            }
        }

        List<DetailedReportProductSalesResponse.ProductSalesData> allProducts = new ArrayList<>(productSalesMap.values());

        // Sort
        Comparator<DetailedReportProductSalesResponse.ProductSalesData> comparator;
        if ("quantity".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt(DetailedReportProductSalesResponse.ProductSalesData::getTotalQuantitySold);
        } else {
            comparator = Comparator.comparing(DetailedReportProductSalesResponse.ProductSalesData::getTotalRevenue);
        }

        if ("ASC".equalsIgnoreCase(sortDir)) {
            // keep ascending
        } else {
            // Default DESC
            comparator = comparator.reversed();
        }
        allProducts.sort(comparator);

        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);
        List<DetailedReportProductSalesResponse.ProductSalesData> pagedProducts = paginateList(allProducts, safePage, safeSize);

        return DetailedReportProductSalesResponse.builder()
                .productsPage(OrderPageResponse.of(pagedProducts, safePage, safeSize, allProducts.size()))
                .build();
    }

    private <T> List<T> paginateList(List<T> items, int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);
        int fromIndex = safePage * safeSize;
        if (fromIndex >= items.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(items.size(), fromIndex + safeSize);
        return items.subList(fromIndex, toIndex);
    }

    private String formatPeriod(LocalDate date, String periodType) {
        if ("WEEKLY".equalsIgnoreCase(periodType)) {
            int week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int weekYear = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
            return weekYear + "-W" + String.format("%02d", week);
        } else if ("MONTHLY".equalsIgnoreCase(periodType)) {
            return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        }
        return date.toString(); // Default DAILY
    }
}
