package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Manual integration test for first Super Admin setup and store locking.
 * Every temporary account/store is removed before exit.
 */
public final class SuperAdminSmokeTest {
    private SuperAdminSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String setupKey = "SmokeSetupKey-16092005";
        String username = "super_smoke_" + System.currentTimeMillis();
        String storeCode = "SASTORE" + System.currentTimeMillis();
        System.setProperty("super.admin.setup.key", setupKey);

        SuperAdminService superAdminService = new SuperAdminService();
        AuthService authService = new AuthService(new JdbcUserDao());
        AppUser superAdmin = null;
        AppUser storeOwner = null;
        try {
            if (!superAdminService.needsSetup()) {
                throw new IllegalStateException(
                        "A real Super Admin already exists; refusing destructive setup smoke test.");
            }
            superAdmin = superAdminService.setupFirst(
                    setupKey, username, "Temporary123!", "Smoke Super Admin");
            if (superAdmin.getRole() != UserRole.SUPER_ADMIN
                    || authService.login("SYSTEM", username, "Temporary123!") == null) {
                throw new IllegalStateException("Super Admin setup/login failed.");
            }

            storeOwner = authService.registerStore(
                    storeCode, "Super Admin Smoke Store", "0900000000", "Test",
                    "owner", "Temporary123!", "Store Owner");
            long storeId = storeOwner.getStoreId();
            List<Store> stores = superAdminService.findStores();
            Store createdStore = stores.stream()
                    .filter(store -> store.getId() == storeId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Super Admin cannot list the new store."));
            if (createdStore.getAdminCount() != 1) {
                throw new IllegalStateException("Store administrator count is incorrect.");
            }

            superAdminService.setStoreActive(storeId, false);
            if (authService.login(storeCode, "owner", "Temporary123!") != null) {
                throw new IllegalStateException("Locked store can still log in.");
            }
            superAdminService.setStoreActive(storeId, true);
            if (authService.login(storeCode, "owner", "Temporary123!") == null) {
                throw new IllegalStateException("Unlocked store cannot log in.");
            }

            System.out.printf(
                    "superAdminSmoke=true, user=%s, store=%s, lockEnforced=true%n",
                    username, storeCode
            );
        } finally {
            if (storeOwner != null) cleanupStore(storeOwner.getStoreId());
            if (superAdmin != null) cleanupSuperAdmin(superAdmin.getId());
            TenantContext.clear();
            System.clearProperty("super.admin.setup.key");
        }
    }

    private static void cleanupStore(long storeId) throws Exception {
        TenantContext.setStoreId(storeId);
        try (Connection connection = DatabaseConnection.getConnection()) {
            execute(connection, "DELETE FROM stock_transactions");
            execute(connection, "DELETE FROM invoice_details");
            execute(connection, "DELETE FROM invoices");
            execute(connection, "DELETE FROM purchase_order_details");
            execute(connection, "DELETE FROM purchase_orders");
            execute(connection, "DELETE FROM discount_codes");
            execute(connection, "DELETE FROM products");
            execute(connection, "DELETE FROM categories");
            execute(connection, "DELETE FROM suppliers");
            execute(connection, "DELETE FROM customers");
            execute(connection, "DELETE FROM app_users");
        }
        TenantContext.clear();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM stores WHERE id=?")) {
            statement.setLong(1, storeId);
            statement.executeUpdate();
        }
    }

    private static void cleanupSuperAdmin(long userId) throws Exception {
        long systemStoreId;
        TenantContext.clear();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT id FROM stores WHERE code='SYSTEM'");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            systemStoreId = resultSet.getLong(1);
        }
        TenantContext.setStoreId(systemStoreId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM app_users WHERE id=?")) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
