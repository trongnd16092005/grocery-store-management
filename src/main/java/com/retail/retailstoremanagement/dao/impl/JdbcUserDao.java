package com.retail.retailstoremanagement.dao.impl;
import com.retail.retailstoremanagement.dao.UserDao;
import com.retail.retailstoremanagement.model.*;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import java.sql.*;
import java.time.OffsetDateTime;

public class JdbcUserDao implements UserDao {
    public AppUser findByUsername(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT u.*,s.code store_code,s.name store_name FROM app_users u "
                             + "JOIN stores s ON s.id=u.store_id "
                             + "WHERE s.active AND LOWER(u.username)=LOWER(?)")) {
            s.setString(1, username);
            try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public AppUser findById(long id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT u.*,s.code store_code,s.name store_name FROM app_users u "
                             + "JOIN stores s ON s.id=u.store_id WHERE s.active AND u.id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public int count() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM app_users")) {
            r.next(); return r.getInt(1);
        }
    }

    public AppUser createAdmin(String username, String hash, String fullName) throws SQLException {
        String sql = "INSERT INTO app_users(username,password_hash,full_name,role,active) " +
                     "SELECT ?,?,?,'ADMIN',TRUE WHERE NOT EXISTS(SELECT 1 FROM app_users) RETURNING *";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, username); s.setString(2, hash); s.setString(3, fullName);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Hệ thống đã có tài khoản.");
                return map(r);
            }
        }
    }

    public AppUser create(String username, String hash, String fullName, UserRole role) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                 "INSERT INTO app_users(username,password_hash,full_name,role) VALUES (?,?,?,?) RETURNING *")) {
            s.setString(1, username); s.setString(2, hash);
            s.setString(3, fullName); s.setString(4, role.name());
            try (ResultSet r = s.executeQuery()) { r.next(); return map(r); }
        }
    }

    public java.util.List<AppUser> findAll() throws SQLException {
        java.util.List<AppUser> users = new java.util.ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM app_users ORDER BY created_at")) {
            while (r.next()) users.add(map(r));
        }
        return users;
    }

    public AppUser update(long id, String fullName, UserRole role) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockActiveAdmins(c);
                AppUser current = lockUser(c, id);
                if (current.getRole() == UserRole.ADMIN && current.isActive()
                        && role != UserRole.ADMIN && countActiveAdmins(c) <= 1) {
                    throw new SQLException("Hệ thống phải còn ít nhất một quản trị viên đang hoạt động.");
                }
                try (PreparedStatement s = c.prepareStatement(
                        "UPDATE app_users SET full_name=?, role=?, "
                                + "auth_version=auth_version + CASE WHEN role<>? THEN 1 ELSE 0 END "
                                + "WHERE id=? RETURNING *")) {
                    s.setString(1, fullName);
                    s.setString(2, role.name());
                    s.setString(3, role.name());
                    s.setLong(4, id);
                    try (ResultSet r = s.executeQuery()) {
                        r.next();
                        AppUser result = map(r);
                        c.commit();
                        return result;
                    }
                }
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException(e);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public AppUser setActive(long id, boolean active) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockActiveAdmins(c);
                AppUser current = lockUser(c, id);
                if (!active && current.isActive() && current.getRole() == UserRole.ADMIN
                        && countActiveAdmins(c) <= 1) {
                    throw new SQLException("Không thể khóa quản trị viên đang hoạt động cuối cùng.");
                }
                try (PreparedStatement s = c.prepareStatement(
                        "UPDATE app_users SET active=?, "
                                + "auth_version=auth_version + CASE WHEN active<>? THEN 1 ELSE 0 END "
                                + "WHERE id=? RETURNING *")) {
                    s.setBoolean(1, active);
                    s.setBoolean(2, active);
                    s.setLong(3, id);
                    try (ResultSet r = s.executeQuery()) {
                        r.next();
                        AppUser result = map(r);
                        c.commit();
                        return result;
                    }
                }
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException(e);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void updatePassword(long id, String passwordHash) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                 "UPDATE app_users SET password_hash=?, must_change_password=FALSE, "
                         + "auth_version=auth_version+1 WHERE id=?")) {
            s.setString(1, passwordHash); s.setLong(2, id);
            if (s.executeUpdate() == 0) throw new SQLException("Không tìm thấy tài khoản.");
        }
    }

    private AppUser map(ResultSet r) throws SQLException {
        AppUser u = new AppUser();
        u.setId(r.getLong("id"));
        u.setStoreId(r.getLong("store_id"));
        try {
            u.setStoreCode(r.getString("store_code"));
            u.setStoreName(r.getString("store_name"));
        } catch (SQLException ignored) {
            // INSERT/UPDATE RETURNING does not include joined store columns.
        }
        u.setUsername(r.getString("username"));
        u.setPasswordHash(r.getString("password_hash"));
        u.setFullName(r.getString("full_name"));
        u.setRole(UserRole.valueOf(r.getString("role")));
        u.setActive(r.getBoolean("active"));
        u.setAuthVersion(r.getInt("auth_version"));
        u.setMustChangePassword(r.getBoolean("must_change_password"));
        u.setCreatedAt(r.getObject("created_at", OffsetDateTime.class));
        u.setUpdatedAt(r.getObject("updated_at", OffsetDateTime.class));
        return u;
    }

    private AppUser lockUser(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM app_users WHERE id=? FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Không tìm thấy tài khoản.");
                return map(resultSet);
            }
        }
    }

    private void lockActiveAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM app_users WHERE role='ADMIN' AND active ORDER BY id FOR UPDATE");
             ResultSet ignored = statement.executeQuery()) {
            while (ignored.next()) {
                // Lock all active administrators for a consistent last-admin check.
            }
        }
    }

    private int countActiveAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM app_users WHERE role='ADMIN' AND active");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
