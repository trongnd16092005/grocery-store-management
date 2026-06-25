package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.service.AuthService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/users")
public class UserServlet extends HttpServlet {
    private final AuthService service = new AuthService(new JdbcUserDao());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            AppUser current = (AppUser) req.getSession().getAttribute("currentUser");
            req.setAttribute("users", current != null && current.getRole() == UserRole.ADMIN
                    ? service.findAll() : java.util.List.of(current));
            req.setAttribute("currentUserId", current != null ? current.getId() : -1L);
            req.setAttribute("isAdmin", current != null && current.getRole() == UserRole.ADMIN);
            req.setAttribute("mustChangePassword",
                    current != null && current.isMustChangePassword());
            req.setAttribute("flashSuccess", RequestUtils.consumeFlash(req, "flashSuccess"));
            req.setAttribute("flashError",   RequestUtils.consumeFlash(req, "flashError"));
            req.getRequestDispatcher("/WEB-INF/views/users.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * POST /users
     * Phân biệt hành động qua tham số ẩn "action":
     *   (trống)          → tạo tài khoản mới  (ADMIN only)
     *   edit             → sửa họ tên / vai trò (ADMIN only)
     *   lock             → khóa tài khoản       (ADMIN only)
     *   unlock           → mở khóa tài khoản    (ADMIN only)
     *   change-password  → đổi mật khẩu (chính mình, cần mật khẩu cũ)
     *   reset-password   → đặt lại mật khẩu người khác (ADMIN only)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        AppUser current = (AppUser) req.getSession().getAttribute("currentUser");
        String action = RequestUtils.text(req, "action");
        try {
            switch (action) {
                case "edit":
                    requireAdmin(current);
                    service.updateUser(
                        RequestUtils.requiredLong(req, "id"),
                        RequestUtils.text(req, "fullName"),
                        RequestUtils.text(req, "role"),
                        current
                    );
                    // Nếu admin đang sửa chính mình, cập nhật session
                    if (current != null && current.getId() == RequestUtils.requiredLong(req, "id")) {
                        current.setFullName(RequestUtils.text(req, "fullName"));
                    }
                    RequestUtils.flash(req, "flashSuccess", "Đã cập nhật tài khoản.");
                    break;

                case "lock":
                    requireAdmin(current);
                    service.toggleActive(RequestUtils.requiredLong(req, "id"), false, current);
                    RequestUtils.flash(req, "flashSuccess", "Đã khóa tài khoản.");
                    break;

                case "unlock":
                    requireAdmin(current);
                    service.toggleActive(RequestUtils.requiredLong(req, "id"), true, current);
                    RequestUtils.flash(req, "flashSuccess", "Đã mở khóa tài khoản.");
                    break;

                case "change-password":
                    if (current == null) throw new IllegalStateException("Chưa đăng nhập.");
                    requireMatchingPasswords(req);
                    service.changeOwnPassword(
                        current.getId(),
                        req.getParameter("currentPassword"),
                        req.getParameter("newPassword")
                    );
                    req.getSession().invalidate();
                    resp.sendRedirect(req.getContextPath() + "/login?passwordChanged=1");
                    return;

                case "reset-password":
                    requireAdmin(current);
                    requireMatchingPasswords(req);
                    service.resetPassword(
                        RequestUtils.requiredLong(req, "id"),
                        req.getParameter("newPassword"),
                        current
                    );
                    RequestUtils.flash(req, "flashSuccess", "Đã đặt lại mật khẩu.");
                    break;

                default: // tạo mới
                    requireAdmin(current);
                    service.createUser(
                        RequestUtils.text(req, "username"),
                        req.getParameter("password"),
                        RequestUtils.text(req, "fullName"),
                        RequestUtils.text(req, "role")
                    );
                    RequestUtils.flash(req, "flashSuccess", "Đã tạo tài khoản.");
                    break;
            }
        } catch (Exception e) {
            RequestUtils.flash(req, "flashError", e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/users");
    }

    private void requireAdmin(AppUser user) {
        if (user == null || user.getRole() != UserRole.ADMIN)
            throw new com.retail.retailstoremanagement.service.ValidationException(
                "Chỉ quản trị viên được thực hiện thao tác này.");
    }

    private void requireMatchingPasswords(HttpServletRequest request) {
        String newPassword = request.getParameter("newPassword");
        String confirmation = request.getParameter("confirmPassword");
        if (newPassword == null || !newPassword.equals(confirmation)) {
            throw new com.retail.retailstoremanagement.service.ValidationException(
                    "Mật khẩu xác nhận không khớp.");
        }
    }
}
