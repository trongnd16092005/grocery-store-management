package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.SuperAdminDao;
import com.retail.retailstoremanagement.model.Store;
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

    private void restoreTenant(Long previous) {
        if (previous == null) TenantContext.clear();
        else TenantContext.setStoreId(previous);
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
}
