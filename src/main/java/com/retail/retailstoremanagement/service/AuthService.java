package com.retail.retailstoremanagement.service;
import com.retail.retailstoremanagement.dao.UserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.List;

public class AuthService {
    private final UserDao dao;
    public AuthService(UserDao dao) { this.dao = dao; }

    public AppUser login(String username, String password) throws SQLException {
        if (username == null || password == null) return null;
        AppUser u = dao.findByUsername(username.trim());
        return u != null && u.isActive() && BCrypt.checkpw(password, u.getPasswordHash()) ? u : null;
    }

    public boolean needsSetup() throws SQLException { return dao.count() == 0; }

    public AppUser setupAdmin(String username, String password, String fullName) throws SQLException {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}"))
            throw new ValidationException("Tên đăng nhập cần 3–50 ký tự.");
        if (password == null || password.length() < 8)
            throw new ValidationException("Mật khẩu cần ít nhất 8 ký tự.");
        if (fullName == null || fullName.trim().length() < 2)
            throw new ValidationException("Vui lòng nhập họ tên.");
        return dao.createAdmin(username.trim(), BCrypt.hashpw(password, BCrypt.gensalt(12)), fullName.trim());
    }

    public AppUser createUser(String username, String password, String fullName, String role) throws SQLException {
        validate(username, password, fullName);
        UserRole userRole;
        try { userRole = UserRole.valueOf(role); }
        catch (Exception e) { throw new ValidationException("Vai trò không hợp lệ."); }
        return dao.create(username.trim(), BCrypt.hashpw(password, BCrypt.gensalt(12)), fullName.trim(), userRole);
    }

    public AppUser updateUser(long id, String fullName, String role) throws SQLException {
        if (fullName == null || fullName.trim().length() < 2)
            throw new ValidationException("Vui lòng nhập họ tên.");
        UserRole userRole;
        try { userRole = UserRole.valueOf(role); }
        catch (Exception e) { throw new ValidationException("Vai trò không hợp lệ."); }
        return dao.update(id, fullName.trim(), userRole);
    }

    public AppUser toggleActive(long id, boolean active, AppUser currentUser) throws SQLException {
        if (currentUser.getId() == id)
            throw new ValidationException("Không thể khóa chính tài khoản đang đăng nhập.");
        return dao.setActive(id, active);
    }

    /**
     * Đổi mật khẩu cho chính mình — yêu cầu xác nhận mật khẩu hiện tại.
     */
    public void changeOwnPassword(long id, String currentPassword, String newPassword) throws SQLException {
        AppUser u = dao.findById(id);
        if (u == null) throw new ValidationException("Tài khoản không tồn tại.");
        if (!BCrypt.checkpw(currentPassword, u.getPasswordHash()))
            throw new ValidationException("Mật khẩu hiện tại không đúng.");
        if (newPassword == null || newPassword.length() < 8)
            throw new ValidationException("Mật khẩu mới cần ít nhất 8 ký tự.");
        dao.updatePassword(id, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
    }

    /**
     * Đặt lại mật khẩu bởi ADMIN (không cần mật khẩu cũ).
     */
    public void resetPassword(long targetId, String newPassword, AppUser currentUser) throws SQLException {
        if (currentUser.getRole() != UserRole.ADMIN)
            throw new ValidationException("Chỉ quản trị viên được đặt lại mật khẩu người khác.");
        if (newPassword == null || newPassword.length() < 8)
            throw new ValidationException("Mật khẩu mới cần ít nhất 8 ký tự.");
        dao.updatePassword(targetId, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
    }

    public List<AppUser> findAll() throws SQLException { return dao.findAll(); }

    private void validate(String username, String password, String fullName) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}"))
            throw new ValidationException("Tên đăng nhập cần 3–50 ký tự.");
        if (password == null || password.length() < 8)
            throw new ValidationException("Mật khẩu cần ít nhất 8 ký tự.");
        if (fullName == null || fullName.trim().length() < 2)
            throw new ValidationException("Vui lòng nhập họ tên.");
    }
}
