package com.example.shippingservice.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Province {
    @JsonProperty("ProvinceID")
    private int provinceID;

    @JsonProperty("ProvinceName")
    private String provinceName;

    @JsonProperty("Status")
    private int status;

    @JsonProperty("NameExtension")
    private List<String> nameExtension;
}
