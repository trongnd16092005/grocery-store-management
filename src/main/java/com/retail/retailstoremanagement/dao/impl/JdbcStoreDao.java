package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.StoreDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class JdbcStoreDao implements StoreDao {
    @Override
    public Long findActiveIdByCode(String code) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM stores WHERE UPPER(code)=UPPER(?) AND active")) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    @Override
    public AppUser register(String storeCode, String storeName, String phone, String address,
                            String username, String passwordHash, String fullName)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long storeId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO stores(code,name,phone,address) VALUES (UPPER(?),?,?,?) RETURNING id")) {
                    statement.setString(1, storeCode);
                    statement.setString(2, storeName);
                    statement.setString(3, phone);
                    statement.setString(4, address);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        storeId = resultSet.getLong(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT set_config('app.current_store_id', ?, true)")) {
                    statement.setString(1, Long.toString(storeId));
                    statement.execute();
                }
                seedStarterData(connection, storeId);
                AppUser user;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO app_users(store_id,username,password_hash,full_name,role,active) "
                                + "VALUES (?,?,?,?, 'ADMIN', TRUE) RETURNING *")) {
                    statement.setLong(1, storeId);
                    statement.setString(2, username);
                    statement.setString(3, passwordHash);
                    statement.setString(4, fullName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        user = map(resultSet, storeCode.toUpperCase(), storeName);
                    }
                }
                connection.commit();
                return user;
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException(exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void seedStarterData(Connection connection, long storeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO categories(store_id,code,name,description) VALUES "
                        + "(?,'DM001','Đồ uống','Nước giải khát và đồ uống'),"
                        + "(?,'DM002','Bánh kẹo','Bánh, kẹo và đồ ăn nhẹ'),"
                        + "(?,'DM003','Thực phẩm','Thực phẩm đóng gói'),"
                        + "(?,'DM004','Gia vị','Gia vị và nguyên liệu')")) {
            for (int index = 1; index <= 4; index++) statement.setLong(index, storeId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO suppliers(store_id,code,name,active) VALUES "
                        + "(?,'NCC001','Nhà cung cấp mặc định',TRUE)")) {
            statement.setLong(1, storeId);
            statement.executeUpdate();
        }
    }

    private AppUser map(ResultSet resultSet, String storeCode, String storeName)
            throws SQLException {
        AppUser user = new AppUser();
        user.setId(resultSet.getLong("id"));
        user.setStoreId(resultSet.getLong("store_id"));
        user.setStoreCode(storeCode);
        user.setStoreName(storeName);
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

    @Override
    public Store findCurrent() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM stores WHERE id=current_store_id() AND active");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new SQLException("Không tìm thấy cửa hàng.");
            return mapStore(resultSet);
        }
    }

    @Override
    public Store updateCurrent(String name, String phone, String address) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE stores SET name=?,phone=?,address=? "
                             + "WHERE id=current_store_id() AND active RETURNING *")) {
            statement.setString(1, name);
            statement.setString(2, phone);
            statement.setString(3, address);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Không tìm thấy cửa hàng.");
                return mapStore(resultSet);
            }
        }
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
