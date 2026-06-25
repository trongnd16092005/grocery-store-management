package com.retail.retailstoremanagement.config;

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

    private static String required(String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        if (value == null || value.isBlank()) value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình " + environmentName + " hoặc -D" + propertyName + ".");
        }
        return value.trim();
    }
}
