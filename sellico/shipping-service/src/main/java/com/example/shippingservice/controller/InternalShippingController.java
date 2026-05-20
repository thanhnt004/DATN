package com.example.shippingservice.controller;

import com.example.shippingservice.dto.ghn.GHNFeeResponse;
import com.example.shippingservice.dto.request.ShippingFeeRequest;
import com.example.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/shipping")
@RequiredArgsConstructor
public class InternalShippingController {
    private final ShippingService shippingService;
    @PostMapping("/calculate-fee")
    public GHNFeeResponse calculateFee(@RequestBody ShippingFeeRequest request) {
        return shippingService.calculateShippingFee(request);
    }
}
