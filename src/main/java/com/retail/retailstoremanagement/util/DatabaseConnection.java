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
        return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUser(),
                DatabaseConfig.getPassword()
        );
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException | IllegalStateException exception) {
            return false;
        }
    }
}
