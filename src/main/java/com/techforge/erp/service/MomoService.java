package com.techforge.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techforge.erp.model.Invoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MomoService {

    @Value("${momo.endpoint}")
    private String momoEndpoint;

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> createPaymentUrl(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice must not be null");

        String invoiceId = invoice.getId() == null ? UUID.randomUUID().toString() : invoice.getId();

        Double amountDouble = invoice.getAmount();
        long amountLong = 0L;
        if (amountDouble != null) {
            amountLong = amountDouble.longValue(); // truncate decimals
            if (amountLong < 0) amountLong = 0L;
        }

        String orderId = invoiceId + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        String orderInfo = "Payment for Invoice " + invoiceId;
        String extraData = "";
        String requestType = "captureWallet";

        String rawSignature = String.format(
                "partnerCode=%s&accessKey=%s&requestId=%s&amount=%d&orderId=%s&orderInfo=%s&returnUrl=%s&notifyUrl=%s&extraData=%s",
                partnerCode, accessKey, requestId, amountLong, orderId, orderInfo, redirectUrl, ipnUrl, extraData
        );

        String signature = hmacSHA256(rawSignature, secretKey);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", partnerCode);
        payload.put("accessKey", accessKey);
        payload.put("requestId", requestId);
        payload.put("amount", String.valueOf(amountLong));
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("returnUrl", redirectUrl);
        payload.put("notifyUrl", ipnUrl);
        payload.put("extraData", extraData);
        payload.put("requestType", requestType);
        payload.put("signature", signature);

        Map<String, Object> result = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(momoEndpoint, request, String.class);
            result.put("statusCode", response.getStatusCode().value());
            if (response.getBody() != null) {
                Map<?, ?> bodyMap = objectMapper.readValue(response.getBody(), Map.class);
                result.put("body", bodyMap);
            } else {
                result.put("body", Collections.emptyMap());
            }
            result.put("requestPayload", payload);
        } catch (Exception e) {
            result.put("error", "Failed to call MoMo API: " + e.getMessage());
            result.put("requestPayload", payload);
        }
        return result;
    }

    private String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to generate HMAC SHA256", ex);
        }
    }
}

