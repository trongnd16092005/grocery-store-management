package com.retail.retailstoremanagement.config;

/**
 * Reads database settings from environment variables or JVM system properties.
 * Environment variables are preferred in deployed environments.
 */
public final class DatabaseConfig {
    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/grocery_store";
    private static final String DEFAULT_USER = "grocery_app";

    private DatabaseConfig() {
    }

    public static String getUrl() {
        return readSetting("DB_URL", "db.url", DEFAULT_URL);
    }

    public static String getUser() {
        return readSetting("DB_USER", "db.user", DEFAULT_USER);
    }

    public static String getPassword() {
        String password = readSetting("DB_PASSWORD", "db.password", null);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Database password is missing. Set DB_PASSWORD or -Ddb.password."
            );
        }
        return password;
    }

    private static String readSetting(String environmentName,
                                      String propertyName,
                                      String defaultValue) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }

        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }
}
