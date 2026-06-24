package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Category;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TenantContext;
import com.retail.retailstoremanagement.util.TestTenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;

public final class MultiStoreIsolationSmokeTest {
    private MultiStoreIsolationSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String code = "SMOKE" + System.currentTimeMillis();
        AuthService auth = new AuthService(new JdbcUserDao());
        AppUser owner = auth.registerStore(
                code, "Isolation Smoke Store", "0900000000", "Test",
                "admin", "Temporary123!", "Store Owner"
        );
        Long storeId = owner.getStoreId();
        try {
            if (auth.login(code, "admin", "Temporary123!") == null) {
                throw new IllegalStateException("New store login failed.");
            }

            TenantContext.setStoreId(storeId);
            if (new ProductService().count("", null, "") != 0) {
                throw new IllegalStateException("New store can see products from another store.");
            }
            if (new CategoryService().findAll().size() != 4) {
                throw new IllegalStateException("Starter categories were not created.");
            }
            Category category = new Category();
            category.setName("Danh mục riêng");
            new CategoryService().save(category);

            TestTenantContext.activateDefaultStore();
            if (new CategoryService().search("Danh mục riêng").size() != 0) {
                throw new IllegalStateException("Default store can see another store's category.");
            }
            System.out.printf("multiStoreSmoke=true, store=%s, isolated=true%n", code);
        } finally {
            cleanup(storeId);
            TenantContext.clear();
        }
    }

    private static void cleanup(long storeId) throws Exception {
        TenantContext.setStoreId(storeId);
        try (Connection connection = DatabaseConnection.getConnection()) {
            execute(connection, "DELETE FROM stock_transactions");
            execute(connection, "DELETE FROM invoice_details");
            execute(connection, "DELETE FROM invoices");
            execute(connection, "DELETE FROM purchase_order_details");
            execute(connection, "DELETE FROM purchase_orders");
            execute(connection, "DELETE FROM products");
            execute(connection, "DELETE FROM categories");
            execute(connection, "DELETE FROM suppliers");
            execute(connection, "DELETE FROM customers");
            execute(connection, "DELETE FROM app_users");
        }
        TenantContext.clear();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM stores WHERE id=?")) {
            statement.setLong(1, storeId);
            statement.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
