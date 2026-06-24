package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.SuperAdminDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcSuperAdminDao implements SuperAdminDao {
    @Override
    public int countSuperAdmins() throws SQLException {
        return withSystemTenant(() -> {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM app_users WHERE role='SUPER_ADMIN'");
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        });
    }

    @Override
    public AppUser createFirstSuperAdmin(String username, String passwordHash, String fullName)
            throws SQLException {
        return withSystemTenant(() -> {
            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    lockSuperAdmins(connection);
                    if (countSuperAdmins(connection) > 0) {
                        throw new SQLException("Super Admin đã được khởi tạo.");
                    }
                    String sql = "INSERT INTO app_users(username,password_hash,full_name,role,active) "
                            + "VALUES (?,?,?,'SUPER_ADMIN',TRUE) RETURNING *";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, username);
                        statement.setString(2, passwordHash);
                        statement.setString(3, fullName);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            resultSet.next();
                            AppUser user = mapUser(resultSet);
                            user.setStoreCode("SYSTEM");
                            user.setStoreName("Quản trị hệ thống");
                            connection.commit();
                            return user;
                        }
                    }
                } catch (Exception exception) {
                    connection.rollback();
                    if (exception instanceof SQLException) throw (SQLException) exception;
                    throw new SQLException(exception);
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    @Override
    public List<Store> findStores() throws SQLException {
        /*
         * app_users uses FORCE RLS, therefore the correlated counts are intentionally
         * populated by a SECURITY DEFINER-free fallback below, one tenant at a time.
         */
        List<Store> stores = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM stores WHERE code<>'SYSTEM' ORDER BY created_at DESC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) stores.add(mapStore(resultSet));
        }
        for (Store store : stores) populateUserCounts(store);
        return stores;
    }

    @Override
    public boolean setStoreActive(long storeId, boolean active) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE stores SET active=? WHERE id=? AND code<>'SYSTEM'")) {
            statement.setBoolean(1, active);
            statement.setLong(2, storeId);
            return statement.executeUpdate() == 1;
        }
    }

    private void populateUserCounts(Store store) throws SQLException {
        Long previous = TenantContext.getStoreId();
        TenantContext.setStoreId(store.getId());
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FILTER (WHERE role='ADMIN' AND active) admin_count,"
                             + "COUNT(*) FILTER (WHERE role='CASHIER' AND active) employee_count "
                             + "FROM app_users");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            store.setAdminCount(resultSet.getLong("admin_count"));
            store.setEmployeeCount(resultSet.getLong("employee_count"));
        } finally {
            restoreTenant(previous);
        }
    }

    private long systemStoreId() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM stores WHERE code='SYSTEM' AND active");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new SQLException("Tenant hệ thống chưa được khởi tạo.");
            return resultSet.getLong(1);
        }
    }

    private <T> T withSystemTenant(SqlSupplier<T> supplier) throws SQLException {
        Long previous = TenantContext.getStoreId();
        TenantContext.setStoreId(systemStoreId());
        try {
            return supplier.get();
        } finally {
            restoreTenant(previous);
        }
    }

    private void restoreTenant(Long previous) {
        if (previous == null) TenantContext.clear();
        else TenantContext.setStoreId(previous);
    }

    private void lockSuperAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_advisory_xact_lock(16092005)")) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM app_users WHERE role='SUPER_ADMIN' FOR UPDATE");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                // Serialize first-account creation.
            }
        }
    }

    private int countSuperAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM app_users WHERE role='SUPER_ADMIN'");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private AppUser mapUser(ResultSet resultSet) throws SQLException {
        AppUser user = new AppUser();
        user.setId(resultSet.getLong("id"));
        user.setStoreId(resultSet.getLong("store_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(UserRole.valueOf(resultSet.getString("role")));
        user.setActive(resultSet.getBoolean("active"));
        user.setAuthVersion(resultSet.getInt("auth_version"));
        user.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        user.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return user;
    }

    private Store mapStore(ResultSet resultSet) throws SQLException {
        Store store = new Store();
        store.setId(resultSet.getLong("id"));
        store.setCode(resultSet.getString("code"));
        store.setName(resultSet.getString("name"));
        store.setPhone(resultSet.getString("phone"));
        store.setAddress(resultSet.getString("address"));
        store.setActive(resultSet.getBoolean("active"));
        store.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        store.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return store;
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }
}
