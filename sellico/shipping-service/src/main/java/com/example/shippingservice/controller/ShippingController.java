package com.example.shippingservice.controller;

import com.example.shippingservice.dto.ghn.District;
import com.example.shippingservice.dto.ghn.GHNFeeResponse;
import com.example.shippingservice.dto.ghn.Province;
import com.example.shippingservice.dto.ghn.Ward;
import com.example.shippingservice.dto.request.PrintOrderRequest;
import com.example.shippingservice.dto.request.ShippingFeeRequest;
import com.example.shippingservice.dto.request.ShippingOrderRequest;
import com.example.shippingservice.dto.response.BatchResult;
import com.example.shippingservice.dto.response.ShippingOrderResponse;
import com.example.shippingservice.service.AddressService;
import com.example.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final AddressService addressService;
    private final ShippingService shippingService;

    @GetMapping("/provinces")
    public List<Province> getProvinces() {
        return addressService.getProvinces();
    }

    @GetMapping("/districts")
    public List<District> getDistricts(@RequestParam int provinceId) {
        return addressService.getDistricts(provinceId);
    }

    @GetMapping("/wards")
    public List<Ward> getWards(@RequestParam int districtId) {
        return addressService.getWards(districtId);
    }
//    @PostMapping("/orders")
//    public ShippingOrderResponse createOrder(@RequestBody ShippingOrderRequest request) {
//        return shippingService.createShippingOrder(request);
//    }
    @PostMapping("/orders")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<BatchResult<UUID>> createShippingOrders(@RequestBody List<UUID> orderIds, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        String sellerId = jwt.getSubject();
        return ApiResponse.success(shippingService.createShippingOrders(orderIds, UUID.fromString(sellerId)));
    }
    @PostMapping("/print")
    public String getPrintLink(@RequestBody PrintOrderRequest request) {
        return shippingService.getPrintToken(request);
    }
}
