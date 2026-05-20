package com.example.shippingservice.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Ward {
    @JsonProperty("WardCode")
    private String wardCode;
    @JsonProperty("WardName")
    private String wardName;
    @JsonProperty("DistrictID")
    private int districtID;
    @JsonProperty("SupportType")
    private int supportType;
    @JsonProperty("Status")
    private int status;
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
}
