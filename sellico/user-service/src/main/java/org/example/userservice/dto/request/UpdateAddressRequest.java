package org.example.userservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAddressRequest {
    @Size(max = 100, message = "Receiver name must be less than 100 characters")
    private String receiverName;

    @Size(max = 20, message = "Receiver phone must be less than 20 characters")
    private String receiverPhone;

    private String province;

    private String district;

    private String ward;

    private String addressLine;
}

