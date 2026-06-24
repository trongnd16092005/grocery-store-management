package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;
import com.retail.retailstoremanagement.util.TestTenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Manual integration test for session invalidation and administrator safeguards. */
public final class UserSecuritySmokeTest {
    private UserSecuritySmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        TestTenantContext.activateDefaultStore();
        JdbcUserDao dao = new JdbcUserDao();
        AuthService auth = new AuthService(dao);
        AppUser user = null;
        try {
            String username = "security_smoke_" + System.currentTimeMillis();
            user = dao.create(username, BCrypt.hashpw("Original123!", BCrypt.gensalt(4)),
                    "Security Smoke", UserRole.CASHIER);
            int version = user.getAuthVersion();

            user = dao.update(user.getId(), user.getFullName(), UserRole.ADMIN);
            assertIncremented(version, user.getAuthVersion(), "role change");
            version = user.getAuthVersion();

            try {
                auth.updateUser(user.getId(), user.getFullName(), "CASHIER", user);
                throw new IllegalStateException("Self-demotion was accepted.");
            } catch (ValidationException expected) {
                // Correct.
            }

            user = dao.setActive(user.getId(), false);
            assertIncremented(version, user.getAuthVersion(), "account lock");
            if (auth.login("CUAHANGABC", username, "Original123!") != null) {
                throw new IllegalStateException("Locked account could still log in.");
            }

            version = user.getAuthVersion();
            user = dao.setActive(user.getId(), true);
            assertIncremented(version, user.getAuthVersion(), "account unlock");

            version = user.getAuthVersion();
            dao.updatePassword(user.getId(), BCrypt.hashpw("Changed123!", BCrypt.gensalt(4)));
            AppUser refreshed = dao.findById(user.getId());
            assertIncremented(version, refreshed.getAuthVersion(), "password change");
            if (auth.login("CUAHANGABC", username, "Original123!") != null
                    || auth.login("CUAHANGABC", username, "Changed123!") == null) {
                throw new IllegalStateException("Password version/login validation failed.");
            }

            System.out.printf(
                    "userSecuritySmoke=true, authVersion=%d, selfDemotionBlocked=true%n",
                    refreshed.getAuthVersion()
            );
        } finally {
            if (user != null) deleteUser(user.getId());
        }

        verifyLastActiveAdminProtection(dao);
    }

    private static void verifyLastActiveAdminProtection(JdbcUserDao dao) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM app_users WHERE role='ADMIN' AND active ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) return;
            long onlyAdminId = resultSet.getLong(1);
            if (resultSet.next()) return;
            try {
                dao.setActive(onlyAdminId, false);
                throw new IllegalStateException("Last active administrator was locked.");
            } catch (SQLException expected) {
                // Correct.
            }
        }
    }

    private static void assertIncremented(int before, int after, String action) {
        if (after != before + 1) {
            throw new IllegalStateException(action + " did not increment auth_version.");
        }
    }

    private static void deleteUser(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM app_users WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
