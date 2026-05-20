package com.example.shippingservice.client;

import com.example.shippingservice.dto.ghn.*;
import com.example.shippingservice.dto.request.PrintOrderRequest;
import com.example.shippingservice.dto.request.ShippingOrderRequest;
import com.example.shippingservice.dto.response.PrintOrderResponse;
import com.example.shippingservice.dto.response.ShippingOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ghn-client", url = "${app.ghn.base-url}")
public interface GHNClient {

    @GetMapping("/master-data/province")
    GHNResponse<List<Province>> getProvinces(@RequestHeader("Token") String token);

    @GetMapping("/master-data/district")
    GHNResponse<List<District>> getDistricts(@RequestHeader("Token") String token,
                                             @RequestBody Map<String, Object> body);

    @GetMapping("/master-data/ward")
    GHNResponse<List<Ward>> getWards(@RequestHeader("Token") String token,
                                     @RequestBody Map<String, Object> body);

    @PostMapping(value = "/v2/shipping-order/fee",consumes = "application/json", produces = "application/json")
    GHNResponse<GHNFeeResponse> calculateFee(@RequestHeader("Token") String token,
                                            @RequestHeader("ShopId") String shopId,
                                            @RequestBody Map<String, Object> request);

    @PostMapping("/v2/shipping-order/create")
    GHNResponse<ShippingOrderResponse> createOrder(@RequestHeader("Token") String token,
                                                  @RequestHeader("ShopId") String shopId,
                                                  @RequestBody ShippingOrderRequest request);

    @PostMapping("/v2/a5/gen-token")
    GHNResponse<PrintOrderResponse> generateTokenPrint(@RequestHeader("Token") String token,
                                                      @RequestBody PrintOrderRequest request);
}
