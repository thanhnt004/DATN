package com.example.shippingservice.service;

import com.example.shippingservice.client.GHNClient;
import com.example.shippingservice.dto.ghn.District;
import com.example.shippingservice.dto.ghn.GHNResponse;
import com.example.shippingservice.dto.ghn.Province;
import com.example.shippingservice.dto.ghn.Ward;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final GHNClient ghnClient;

    @Value("${app.ghn.token}")
    private String token;

    public List<Province> getProvinces() {
        GHNResponse<List<Province>> response = ghnClient.getProvinces(token);
        return response.getData();
    }

    public List<District> getDistricts(int provinceId) {
        GHNResponse<List<District>> response = ghnClient.getDistricts(token, Map.of("province_id", provinceId));
        return response.getData();
    }

    public List<Ward> getWards(int districtId) {
        GHNResponse<List<Ward>> response = ghnClient.getWards(token, Map.of("district_id", districtId));
        return response.getData();
    }
}
