package com.example.paymentservice.infrastructure.vnpay;

import com.example.paymentservice.application.port.out.VnPayGatewayPort;
import com.example.paymentservice.config.VnPayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class VnPayGatewayAdapter implements VnPayGatewayPort {

    private final VnPayConfig config;

    private static final DateTimeFormatter VN_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String buildPaymentUrl(String vnpTxnRef, BigDecimal amount, String orderInfo, String ipAddress) {
        Map<String, String> params = new TreeMap<>();

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String createDate = now.format(VN_DATE_FORMAT);
        String expireDate = now.plusMinutes(15).format(VN_DATE_FORMAT);

        // VNPay amount is in VND * 100 (no decimals)
        long vnpAmount = amount.longValue() * 100;

        params.put("vnp_Version", config.getVersion());
        params.put("vnp_Command", config.getCommand());
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", vnpTxnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", config.getOrderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // Build query string and hash
        String queryString = VnPayUtils.buildQueryString(params);
        String secureHash = VnPayUtils.hmacSHA512(config.getHashSecret(), queryString);

        String paymentUrl = config.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
        log.info("VNPay payment URL generated for txnRef={}", vnpTxnRef);
        return paymentUrl;
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        // Remove hash fields before re-calculating
        Map<String, String> fieldsToHash = new TreeMap<>(params);
        fieldsToHash.remove("vnp_SecureHash");
        fieldsToHash.remove("vnp_SecureHashType");

        String calculatedHash = VnPayUtils.hashAllFields(fieldsToHash, config.getHashSecret());
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }
}

