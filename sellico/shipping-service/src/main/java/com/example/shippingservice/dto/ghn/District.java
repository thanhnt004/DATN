package com.example.shippingservice.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;


@Data
public class District {
    @JsonProperty("DistrictID")
    private int districtID;
    @JsonProperty("DistrictName")
    private String districtName;
    @JsonProperty("ProvinceID")
    private int provinceID;
    @JsonProperty("Status")
    private int status;
    @JsonProperty("SupportType")
    private int supportType;
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
}
