package com.example.paymentservice.application.port.out;

import java.math.BigDecimal;
import java.util.Map;

public interface VnPayGatewayPort {
    String buildPaymentUrl(String vnpTxnRef, BigDecimal amount, String orderInfo, String ipAddress);
    boolean verifySignature(Map<String, String> params);
}

