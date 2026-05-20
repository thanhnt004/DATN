package com.example.shippingservice.service;

import com.example.shippingservice.client.GHNClient;
import com.example.shippingservice.client.OrderClient;
import com.example.shippingservice.client.OrderResponse;
import com.example.shippingservice.client.ProductClient;
import com.example.shippingservice.client.SellerClient;
import com.example.shippingservice.dto.ghn.GHNFeeResponse;
import com.example.shippingservice.dto.ghn.GHNResponse;
import com.example.shippingservice.dto.request.PrintOrderRequest;
import com.example.shippingservice.dto.request.ShippingFeeRequest;
import com.example.shippingservice.dto.request.ShippingOrderRequest;
import com.example.shippingservice.dto.response.BatchResult;
import com.example.shippingservice.dto.response.PrintOrderResponse;
import com.example.shippingservice.dto.response.SellerResponse;
import com.example.shippingservice.dto.response.ShippingOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import response.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final GHNClient ghnClient;
    private final SellerClient sellerClient;
    private final GHNService ghnService;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    @Value("${app.ghn.token}")
    private String token;

    @Value("${app.ghn.shop-id}")
    private String shopId;

    public GHNFeeResponse calculateShippingFee(ShippingFeeRequest request) {
        Map<String, Object> ghnRequest = new HashMap<>();
        
//        // Mặc định service_type_id = 2 (E-commerce delivery) nếu không có service_id
//        if (request.getServiceId() != null) {
//            ghnRequest.put("service_id", request.getServiceId());
//        } else {
//            ghnRequest.put("service_type_id", request.getServiceTypeId() != null ? request.getServiceTypeId() : 2);
//        }
        ghnRequest.put("service_type_id",2);
        ghnRequest.put("service_id",null);
        var ward = ghnService.getWard(request.getWard(), request.getCity(), request.getDistrict());
        ghnRequest.put("to_district_id", ward.getDistrictID());
        ghnRequest.put("to_ward_code", ward.getWardCode());
        try{
            // Lấy địa chỉ từ seller service nếu có sellerId
            if (request.getSellerId() != null) {
                try {
                    ApiResponse<SellerResponse> sellerApiResponse = sellerClient.getSeller(request.getSellerId());
                    if (sellerApiResponse != null && sellerApiResponse.getData() != null) {
                        SellerResponse seller = sellerApiResponse.getData();
                        log.info("Seller: {}", seller);
                        var sellerWard = ghnService.getWard(seller.getWard(), seller.getCity(), seller.getDistrict());
                        log.info("Seller Ward: {}", sellerWard);
                        ghnRequest.put("from_district_id", sellerWard.getDistrictID());
                        ghnRequest.put("from_ward_code", sellerWard.getWardCode());
                    }
                } catch (Exception e) {
                    log.error("Error fetching seller info from seller-service for sellerId: {}", request.getSellerId(), e);
                    throw new RuntimeException("Error fetching seller info from seller-service ");
                }
            }else {
                throw new RuntimeException("SellerId is required");
            }
            ghnRequest.put("weight", request.getWeight());
            ghnRequest.put("length", request.getLength());
            ghnRequest.put("width", request.getWidth());
            ghnRequest.put("height", request.getHeight());
            ghnRequest.put("insurance_value", 0);
            log.info("GHN Request: {}", ghnRequest);
            GHNResponse<GHNFeeResponse> response = ghnClient.calculateFee(token, shopId, ghnRequest);
            return response.getData();
        }catch (Exception e){
            log.warn("Error calculating shipping fee: {}, USER PLACE HOLDER", e.getMessage());
            return GHNFeeResponse.builder()
                    .insuranceFee(0)
                    .pickStationFee(0)
                    .total(30000)
                    .serviceFee(30000)
                    .r2sFee(0)
                    .build();
        }
    }
    public BatchResult<UUID> createShippingOrders(List<UUID> orderIds, UUID sellerId) {
        List<OrderResponse> orderResponses = orderClient.getOrderByOrderId(orderIds).getData();
        BatchResult<UUID> result = new BatchResult<UUID>();
        orderResponses.forEach(order -> {
            if (!sellerId.equals(order.getSellerId())) {
                result.addFailure(order.getId(), "Order does not belong to the seller");
                return;
            }
            String errorMsg = createShippingOrder(order);
            if (errorMsg == null) {
                result.addSuccess(order.getId());
            } else {
                result.addFailure(order.getId(), errorMsg);
            }
        });
        return result;

    }
    public String createShippingOrder(OrderResponse order) {
        ShippingOrderRequest request = new ShippingOrderRequest();

        request.setFromName("TinTest124");
        request.setFromPhone("0987654321");
        request.setFromAddress("72 Thành Thái, Phường 14, Quận 10, Hồ Chí Minh, Vietnam");
        request.setFromWardName("Phường 14");
        request.setFromDistrictName("Quận 10");
        request.setFromProvinceName("HCM");

        request.setToName(order.getRecipientName());
        request.setToPhone(order.getRecipientPhone());
        request.setToAddress(order.getShippingAddress());
        request.setToDistrictId(1542);
        request.setToWardCode("1B1507");

        // Mặc định các thông số khác nếu chưa có
        if (request.getPaymentTypeId() == null) request.setPaymentTypeId(2); // Buyer trả phí
        if (request.getRequiredNote() == null) request.setRequiredNote("KHONGCHOXEMHANG");

        List<ShippingOrderRequest.Item> requestItems = null;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<UUID> skuIds = order.getItems().stream().map(OrderResponse.OrderItemResponse::getSkuId).collect(Collectors.toList());
            try {
                List<ProductClient.SkuDetailResponse> skuDetails = productClient.getBatchSkus(new ProductClient.BatchSkusRequest(skuIds, null)).getData();
                if (skuDetails != null) {
                    Map<UUID, ProductClient.SkuDetailResponse> skuMap = skuDetails.stream().collect(Collectors.toMap(ProductClient.SkuDetailResponse::getId, s -> s));
                    requestItems = order.getItems().stream().map(item -> {
                        ProductClient.SkuDetailResponse sku = skuMap.get(item.getSkuId());
                        return ShippingOrderRequest.Item.builder()
                                .name(sku != null && sku.getProductName() != null ? sku.getProductName() : "Product")
                                .code(sku != null && sku.getSkuCode() != null ? sku.getSkuCode() : item.getSkuId().toString())
                                .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
                                .price(item.getUnitPrice() != null ? item.getUnitPrice().intValue() : (sku != null && sku.getPrice() != null ? sku.getPrice().intValue() : 0))
                                .weight(sku != null && sku.getWeightGram() != null ? sku.getWeightGram() : 200)
                                .build();
                    }).collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.error("Error fetching skus from product-service", e);
            }
        }

        if (requestItems == null || requestItems.isEmpty()) {
            requestItems = List.of(ShippingOrderRequest.Item.builder()
                    .name("Product")
                    .code("DEFAULT")
                    .quantity(1)
                    .price(order.getSubtotal() != null ? order.getSubtotal().intValue() : 0)
                    .weight(1000)
                    .build());
        }

        request.setItems(requestItems);

        int totalWeight = requestItems.stream().mapToInt(i -> i.getWeight() * i.getQuantity()).sum();
        request.setWeight(totalWeight > 0 ? totalWeight : 1000);
        request.setLength(10);
        request.setWidth(10);
        request.setHeight(10);

        if(order.getPaymentMethod() != null && order.getPaymentMethod().equals(OrderResponse.PaymentMethod.COD) && order.getSubtotal() != null)
            request.setCodAmount(order.getSubtotal().intValue());

        GHNResponse<ShippingOrderResponse> response = ghnClient.createOrder(token, shopId, request);

        if (response.getCode() != 200) {
            log.error("GHN Error: {}", response.getMessage());
            return response.getMessage();
        }

        try {
            com.example.shippingservice.dto.request.ShipOrderRequest shipReq = com.example.shippingservice.dto.request.ShipOrderRequest.builder()
                    .shippingProvider("GHN")
                    .sellerId(order.getSellerId())
                    .trackingNumber(response.getData().getOrderCode())
                    .note(request.getRequiredNote())
                    .build();
            orderClient.shipOrder(order.getId(), shipReq);
        } catch (Exception e) {
            log.error("Error updating order status in order-service", e);
            return "Failed to update order status in order-service: " + e.getMessage();
        }

        return null;
    }

    public String getPrintToken(PrintOrderRequest request) {
        GHNResponse<PrintOrderResponse> response = ghnClient.generateTokenPrint(token, request);
        
        if (response.getCode() != 200) {
            log.error("GHN Error: {}", response.getMessage());
            throw new RuntimeException("Lỗi lấy token in GHN: " + response.getMessage());
        }
        
        // API In phiếu của GHN yêu cầu token và gọi qua URL: 
        // https://dev-online-gateway.ghn.vn/a5/public-api/printA5?token={token}
        return "https://dev-online-gateway.ghn.vn/a5/public-api/printA5?token=" + response.getData().getToken();
    }
}
