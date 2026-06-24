package com.retail.retailstoremanagement.util;

import com.retail.retailstoremanagement.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection factory.
 * Callers own returned connections and must close them with try-with-resources.
 */
public final class DatabaseConnection {
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    static {
        try {
            Class.forName(POSTGRESQL_DRIVER);
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(
                    "PostgreSQL JDBC driver was not found on the classpath."
            );
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUser(),
                DatabaseConfig.getPassword()
        );
        Long storeId = TenantContext.getStoreId();
        if (storeId != null) {
            try (java.sql.PreparedStatement statement = connection.prepareStatement(
                    "SELECT set_config('app.current_store_id', ?, false)")) {
                statement.setString(1, storeId.toString());
                statement.execute();
            }
        }
        return connection;
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException | IllegalStateException exception) {
            return false;
        }
    }
}
