package com.retail.retailstoremanagement.config;

import java.math.BigDecimal;

public final class PayOsConfig {
    private static final String API_BASE_URL = "https://api-merchant.payos.vn";

    private PayOsConfig() {
    }

    public static String getClientId() {
        return required("PAYOS_CLIENT_ID", "payos.client.id");
    }

    public static String getApiKey() {
        return required("PAYOS_API_KEY", "payos.api.key");
    }

    public static String getChecksumKey() {
        return required("PAYOS_CHECKSUM_KEY", "payos.checksum.key");
    }

    public static String getAppBaseUrl() {
        String value = required("APP_BASE_URL", "app.base.url");
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public static String getApiBaseUrl() {
        return API_BASE_URL;
    }

    public static BigDecimal getTestAmount() {
        String value = optional("PAYOS_TEST_AMOUNT", "payos.test.amount");
        if (value == null) return BigDecimal.valueOf(5000);
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) return null;
        return amount;
    }

    private static String required(String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        if (value == null || value.isBlank()) value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình " + environmentName + " hoặc -D" + propertyName + ".");
        }
        return value.trim();
    }

    private static String optional(String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        if (value == null || value.isBlank()) value = System.getProperty(propertyName);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
