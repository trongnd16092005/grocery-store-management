package com.retail.retailstoremanagement.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Manual smoke test. Run with DB_PASSWORD set in the environment.
 */
public final class DatabaseConnectionSmokeTest {
    private DatabaseConnectionSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        TestTenantContext.activateDefaultStore();
        String sql = "SELECT current_database(), current_user, COUNT(*) FROM products "
                + "GROUP BY current_database(), current_user";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Database smoke test returned no rows.");
            }

            System.out.printf(
                    "database=%s, user=%s, products=%d, valid=%s%n",
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getLong(3),
                    connection.isValid(5)
            );
        }
    }
}
