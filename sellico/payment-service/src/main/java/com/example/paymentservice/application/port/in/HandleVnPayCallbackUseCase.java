package com.example.paymentservice.application.port.in;

import java.util.Map;

public interface HandleVnPayCallbackUseCase {

    record IpnResult(String rspCode, String message) {}
    record ReturnResult(boolean success, String message, String orderId) {}

    IpnResult handleIpn(Map<String, String> params);
    ReturnResult handleReturn(Map<String, String> params);
}

