package com.retail.retailstoremanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.retail.retailstoremanagement.config.PayOsConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PayOsClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String clientId;
    private final String apiKey;
    private final String checksumKey;

    public PayOsClient() {
        this(PayOsConfig.getClientId(), PayOsConfig.getApiKey(),
                PayOsConfig.getChecksumKey());
    }

    public PayOsClient(String clientId, String apiKey, String checksumKey) {
        this.clientId = required("Client ID", clientId);
        this.apiKey = required("API Key", apiKey);
        this.checksumKey = required("Checksum Key", checksumKey);
    }

    public PaymentLink createPayment(long orderCode, long amount, String description,
                                     OffsetDateTime expiresAt) throws Exception {
        String returnUrl = PayOsConfig.getAppBaseUrl() + "/payment/return";
        String cancelUrl = PayOsConfig.getAppBaseUrl() + "/payment/cancel";
        String signatureData = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;

        ObjectNode body = JSON.createObjectNode();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("expiredAt", expiresAt.toEpochSecond());
        body.put("signature", hmac(signatureData));

        JsonNode response = send("/v2/payment-requests", "POST", body.toString());
        if (!"00".equals(response.path("code").asText()) || response.path("data").isMissingNode()) {
            throw new ValidationException(
                    "payOS từ chối tạo mã QR: " + response.path("desc").asText("Không rõ lỗi."));
        }
        JsonNode data = response.path("data");
        return new PaymentLink(
                data.path("paymentLinkId").asText(),
                data.path("checkoutUrl").asText(),
                data.path("qrCode").asText()
        );
    }

    public void cancelPayment(long orderCode, String reason) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("cancellationReason", reason);
        JsonNode response = send("/v2/payment-requests/" + orderCode + "/cancel",
                "POST", body.toString());
        if (!"00".equals(response.path("code").asText())) {
            throw new ValidationException(
                    "Không thể hủy mã QR trên payOS: "
                            + response.path("desc").asText("Không rõ lỗi."));
        }
    }

    public JsonNode parse(String json) throws Exception {
        return JSON.readTree(json);
    }

    public boolean verifyWebhook(JsonNode data, String suppliedSignature) {
        if (data == null || !data.isObject() || suppliedSignature == null) return false;
        String expected = hmac(toSignatureData(data));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                suppliedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private JsonNode send(String path, String method, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(PayOsConfig.getApiBaseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("x-client-id", clientId)
                .header("x-api-key", apiKey);
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        HttpResponse<String> response = http.send(
                builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ValidationException(
                    "Không thể kết nối payOS (HTTP " + response.statusCode() + ").");
        }
        return JSON.readTree(response.body());
    }

    private String toSignatureData(JsonNode data) {
        List<String> keys = new ArrayList<>();
        data.fieldNames().forEachRemaining(keys::add);
        Collections.sort(keys);
        StringBuilder result = new StringBuilder();
        for (String key : keys) {
            if (result.length() > 0) result.append('&');
            result.append(key).append('=').append(signatureValue(data.get(key)));
        }
        return result.toString();
    }

    private String signatureValue(JsonNode value) {
        if (value == null || value.isNull()
                || "null".equals(value.asText()) || "undefined".equals(value.asText())) {
            return "";
        }
        if (value.isArray()) {
            StringBuilder json = new StringBuilder("[");
            for (int index = 0; index < value.size(); index++) {
                if (index > 0) json.append(',');
                json.append(canonicalJson(value.get(index)));
            }
            return json.append(']').toString();
        }
        if (value.isObject()) return canonicalJson(value);
        return value.asText();
    }

    private String canonicalJson(JsonNode value) {
        if (value == null || value.isNull()) return "null";
        if (value.isTextual()) {
            try {
                return JSON.writeValueAsString(value.asText());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
        if (value.isNumber() || value.isBoolean()) return value.toString();
        if (value.isArray()) {
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < value.size(); index++) {
                if (index > 0) result.append(',');
                result.append(canonicalJson(value.get(index)));
            }
            return result.append(']').toString();
        }
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = value.fieldNames();
        iterator.forEachRemaining(keys::add);
        Collections.sort(keys);
        StringBuilder result = new StringBuilder("{");
        for (String key : keys) {
            if (result.length() > 1) result.append(',');
            try {
                result.append(JSON.writeValueAsString(key)).append(':')
                        .append(canonicalJson(value.get(key)));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
        return result.append('}').toString();
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    checksumKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo chữ ký payOS.", exception);
        }
    }

    private String required(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình payOS: " + label + ".");
        }
        return value.trim();
    }

    public static final class PaymentLink {
        private final String paymentLinkId;
        private final String checkoutUrl;
        private final String qrCode;

        public PaymentLink(String paymentLinkId, String checkoutUrl, String qrCode) {
            this.paymentLinkId = paymentLinkId;
            this.checkoutUrl = checkoutUrl;
            this.qrCode = qrCode;
        }

        public String getPaymentLinkId() { return paymentLinkId; }
        public String getCheckoutUrl() { return checkoutUrl; }
        public String getQrCode() { return qrCode; }
    }
}
