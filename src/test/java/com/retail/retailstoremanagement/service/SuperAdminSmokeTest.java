package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TenantContext;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Manual integration test for the single, protected Super Admin account. */
public final class SuperAdminSmokeTest {
    private SuperAdminSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        long systemStoreId = findSystemStoreId();
        TenantContext.setStoreId(systemStoreId);
        try {
            AppUser superAdmin = findOnlySuperAdmin();
            if (superAdmin.getRole() != UserRole.SUPER_ADMIN
                    || !superAdmin.isActive()
                    || !"admin".equalsIgnoreCase(superAdmin.getUsername())) {
                throw new IllegalStateException("Canonical Super Admin is invalid.");
            }

            verifySecondSuperAdminIsRejected();
            verifySuperAdminCannotBeLocked(superAdmin.getId());

            System.out.printf(
                    "superAdminSmoke=true, username=%s, singleton=true, protected=true%n",
                    superAdmin.getUsername()
            );
        } finally {
            TenantContext.clear();
        }
    }

    private static long findSystemStoreId() throws SQLException {
        TenantContext.clear();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT id FROM stores WHERE code='SYSTEM'");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new IllegalStateException("SYSTEM store is missing.");
            return resultSet.getLong(1);
        }
    }

    private static AppUser findOnlySuperAdmin() throws SQLException {
        JdbcUserDao dao = new JdbcUserDao();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM app_users WHERE role='SUPER_ADMIN'");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            if (resultSet.getInt(1) != 1) {
                throw new IllegalStateException("System must contain exactly one Super Admin.");
            }
        }
        return dao.findByUsername("admin");
    }

    private static void verifySecondSuperAdminIsRejected() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO app_users(username,password_hash,full_name,role,active) "
                             + "VALUES (?,?,?,'SUPER_ADMIN',TRUE)")) {
            statement.setString(1, "forbidden_super_admin");
            statement.setString(2, BCrypt.hashpw("Temporary123!", BCrypt.gensalt(4)));
            statement.setString(3, "Forbidden Super Admin");
            try {
                statement.executeUpdate();
                throw new IllegalStateException("A second Super Admin was accepted.");
            } catch (SQLException expected) {
                // The partial unique index must reject this insert.
            }
        }
    }

    private static void verifySuperAdminCannotBeLocked(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("UPDATE app_users SET active=FALSE WHERE id=?")) {
            statement.setLong(1, id);
            try {
                statement.executeUpdate();
                throw new IllegalStateException("The Super Admin account was locked.");
            } catch (SQLException expected) {
                // The protection trigger must reject this update.
            }
        }
    }
}
