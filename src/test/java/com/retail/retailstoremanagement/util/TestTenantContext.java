package com.retail.retailstoremanagement.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public final class TestTenantContext {
    private TestTenantContext() {
    }

    public static void activateDefaultStore() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT id FROM stores WHERE code='CUAHANGABC'")) {
            if (!resultSet.next()) throw new IllegalStateException("Default store was not found.");
            TenantContext.setStoreId(resultSet.getLong(1));
        }
    }
}
