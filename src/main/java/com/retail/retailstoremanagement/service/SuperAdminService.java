package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.config.DatabaseConfig;
import com.retail.retailstoremanagement.dao.SuperAdminDao;
import com.retail.retailstoremanagement.dao.impl.JdbcSuperAdminDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Store;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.List;

public class SuperAdminService {
    private final SuperAdminDao dao;

    public SuperAdminService() {
        this(new JdbcSuperAdminDao());
    }

    public SuperAdminService(SuperAdminDao dao) {
        this.dao = dao;
    }

    public boolean needsSetup() throws SQLException {
        return dao.countSuperAdmins() == 0;
    }

    public AppUser setupFirst(String suppliedSetupKey, String username,
                              String password, String fullName) throws SQLException {
        String configuredKey = DatabaseConfig.getSuperAdminSetupKey();
        if (configuredKey == null || configuredKey.length() < 12) {
            throw new ValidationException(
                    "Chưa cấu hình SUPER_ADMIN_SETUP_KEY (tối thiểu 12 ký tự).");
        }
        if (!constantTimeEquals(configuredKey, suppliedSetupKey)) {
            throw new ValidationException("Khóa khởi tạo Super Admin không đúng.");
        }
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new ValidationException("Tên đăng nhập cần 3–50 ký tự.");
        }
        if (password == null || password.length() < 10) {
            throw new ValidationException("Mật khẩu Super Admin cần ít nhất 10 ký tự.");
        }
        if (fullName == null || fullName.trim().length() < 2) {
            throw new ValidationException("Vui lòng nhập họ tên.");
        }
        return dao.createFirstSuperAdmin(
                username.trim(),
                BCrypt.hashpw(password, BCrypt.gensalt(12)),
                fullName.trim()
        );
    }

    public List<Store> findStores() throws SQLException {
        return dao.findStores();
    }

    public void setStoreActive(long storeId, boolean active) throws SQLException {
        if (!dao.setStoreActive(storeId, active)) {
            throw new ValidationException("Không tìm thấy cửa hàng cần cập nhật.");
        }
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (supplied == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8)
        );
    }
}
