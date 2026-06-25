package com.retail.retailstoremanagement.util;

import com.retail.retailstoremanagement.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Central JDBC connection factory.
 * Callers own returned connections and must close them with try-with-resources.
 */
public final class DatabaseConnection {
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DATA_SOURCE.getConnection();
        Long storeId = TenantContext.getStoreId();
        try {
            // A pooled PostgreSQL session may have served another tenant before.
            // Always overwrite the setting, including requests without a tenant.
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT set_config('app.current_store_id', ?, false)")) {
                statement.setString(1, storeId == null ? "" : storeId.toString());
                statement.execute();
            }
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException | IllegalStateException exception) {
            return false;
        }
    }

    public static void closePool() {
        if (!DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("grocery-store-db");
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(DatabaseConfig.getUrl());
        config.setUsername(DatabaseConfig.getUser());
        config.setPassword(DatabaseConfig.getPassword());
        config.setMaximumPoolSize(readInt("DB_POOL_MAX_SIZE", 6));
        config.setMinimumIdle(readInt("DB_POOL_MIN_IDLE", 1));
        config.setConnectionTimeout(readLong("DB_POOL_CONNECTION_TIMEOUT_MS", 15_000L));
        config.setIdleTimeout(readLong("DB_POOL_IDLE_TIMEOUT_MS", 600_000L));
        config.setMaxLifetime(readLong("DB_POOL_MAX_LIFETIME_MS", 1_500_000L));
        config.setKeepaliveTime(readLong("DB_POOL_KEEPALIVE_MS", 120_000L));
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    private static int readInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(toPropertyName(name));
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " phải là số nguyên.", exception);
        }
    }

    private static long readLong(String name, long defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(toPropertyName(name));
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " phải là số nguyên.", exception);
        }
    }

    private static String toPropertyName(String environmentName) {
        return environmentName.toLowerCase().replace('_', '.');
    }
}
