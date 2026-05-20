package com.example.shippingservice.service;

import com.example.shippingservice.client.GHNClient;
import com.example.shippingservice.dto.ghn.District;
import com.example.shippingservice.dto.ghn.Province;
import com.example.shippingservice.dto.ghn.Ward;
import com.example.shippingservice.exception.ShippingErrorCode;
import com.example.shippingservice.exception.ShippingServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class GHNService {
    private final GHNClient ghnClient;
    @Value("${app.ghn.token}")
    private String ghnToken;
    public Ward getWard(String wardNameInput, String cityName, String districtName) {
        District district = getDistrictByName(cityName, districtName);
        if (district == null) {
            throw new ShippingServiceException(ShippingErrorCode.SHIPPING_DISTRICT_NOT_FOUND);
        }
        // Gọi Feign Client lấy danh sách phường/xã
        var response = ghnClient.getWards(ghnToken, Map.of("district_id", district.getDistrictID()));

        return response.getData().stream()
                .filter(w -> matchesName(wardNameInput, w.getNameExtension(), w.getWardName()))
                .findFirst()
                .orElseThrow(() -> new ShippingServiceException(ShippingErrorCode.SHIPPING_WARD_NOT_FOUND));
    }

    public District getDistrictByName(String cityName, String districtName) {
        Province province = getProvinceByName(cityName);
        if (province == null) return null;

        var response = ghnClient.getDistricts(ghnToken, Map.of("province_id", province.getProvinceID()));

        return response.getData().stream()
                .filter(d -> matchesName(districtName, d.getNameExtension(), d.getDistrictName()))
                .findFirst()
                .orElse(null);
    }

    public Province getProvinceByName(String cityName) {
        var response = ghnClient.getProvinces(ghnToken);

        return response.getData().stream()
                .filter(p -> p.getProvinceID() < 286) // Bỏ qua deprecated
                .filter(p -> matchesName(cityName, p.getNameExtension(), p.getProvinceName()))
                .findFirst()
                .orElse(null);
    }
    private boolean matchesName(String inputName, List<String> extensions, String officialName) {
        if (inputName == null || inputName.isBlank()) return false;

        // Chuẩn hóa chuỗi nhập vào (viết thường, xóa khoảng trắng đầu cuối)
        String cleanInput = inputName.trim().toLowerCase();

        // 1. Kiểm tra với tên chính thức
        if (officialName != null && officialName.toLowerCase().contains(cleanInput)) {
            return true;
        }

        // 2. Kiểm tra trong danh sách tên mở rộng (NameExtension)
        if (extensions != null) {
            return extensions.stream()
                    .anyMatch(ext -> ext.toLowerCase().contains(cleanInput));
        }

        return false;
    }
}
