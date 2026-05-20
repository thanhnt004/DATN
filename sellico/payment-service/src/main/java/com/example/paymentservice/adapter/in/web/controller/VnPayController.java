package com.example.paymentservice.adapter.in.web.controller;

import com.example.paymentservice.application.port.in.HandleVnPayCallbackUseCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * VNPay-specific endpoints:
 * - GET  /api/v1/payments/vnpay/ipn     (public)         → IPN callback from VNPay server
 * - GET  /api/v1/payments/vnpay/return  (public)         → user redirect back
 */
@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VnPayController {

    private final HandleVnPayCallbackUseCase handleVnPayCallbackUseCase;

    @Value("${app.frontend.payment-result-url:http://localhost:3000/payment/result}")
    private String frontendPaymentResultUrl;

    // =====================================================
    // VNPay IPN Callback (server-to-server)
    // =====================================================

    /**
     * VNPay calls this endpoint to confirm payment result.
     * Must return {"RspCode":"00","Message":"Confirm Success"} on success.
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleIpn(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN callback received: vnp_TxnRef={}, vnp_ResponseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        HandleVnPayCallbackUseCase.IpnResult result = handleVnPayCallbackUseCase.handleIpn(params);

        Map<String, String> response = new HashMap<>();
        response.put("RspCode", result.rspCode());
        response.put("Message", result.message());
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // VNPay Return URL (user redirect)
    // =====================================================

    /**
     * User is redirected here after completing payment on VNPay.
     * We verify the result and redirect to the frontend payment result page.
     */
    @GetMapping("/return")
    public void handleReturn(@RequestParam Map<String, String> params, HttpServletResponse response)
            throws IOException {
        log.info("VNPay return callback: vnp_TxnRef={}, vnp_ResponseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        HandleVnPayCallbackUseCase.ReturnResult result = handleVnPayCallbackUseCase.handleReturn(params);

        // Redirect to frontend with result
        String redirectUrl = frontendPaymentResultUrl
                + "?success=" + result.success()
                + "&message=" + result.message()
                + (result.orderId() != null ? "&orderId=" + result.orderId() : "");

        response.sendRedirect(redirectUrl);
    }

    // =====================================================
    // Helpers
    // =====================================================

}

