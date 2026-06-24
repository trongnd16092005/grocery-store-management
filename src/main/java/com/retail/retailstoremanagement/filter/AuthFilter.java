package com.retail.retailstoremanagement.filter;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import com.retail.retailstoremanagement.util.TenantContext;

public class AuthFilter implements Filter {
    private static final Set<String> PUBLIC =
            Set.of("/login", "/register", "/super-admin/setup");
    private static final Set<String> ADMIN = Set.of(
            "/products", "/products.html", "/categories", "/categories.html",
            "/inventory", "/inventory.html", "/suppliers", "/store", "/purchase-orders",
            "/discount-codes"
    );
    private static final Map<String, String> LEGACY = Map.of(
            "/products.html", "/products",
            "/categories.html", "/categories",
            "/inventory.html", "/inventory",
            "/customers.html", "/customers",
            "/invoices.html", "/invoices",
            "/sale.html", "/sale",
            "/login.html", "/login"
    );
    private final JdbcUserDao userDao = new JdbcUserDao();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        TenantContext.clear();
        String path = httpRequest.getServletPath();

        if (LEGACY.containsKey(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + LEGACY.get(path));
            return;
        }
        if (PUBLIC.contains(path) || path.startsWith("/assets/") || path.equals("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession();
        AppUser sessionUser = (AppUser) session.getAttribute("currentUser");
        if (sessionUser == null) {
            requireLogin(httpRequest, httpResponse, path);
            return;
        }

        TenantContext.setStoreId(sessionUser.getStoreId());
        AppUser currentUser;
        try {
            currentUser = userDao.findById(sessionUser.getId());
        } catch (SQLException exception) {
            TenantContext.clear();
            throw new ServletException("Không thể kiểm tra trạng thái tài khoản.", exception);
        }

        if (currentUser == null || !currentUser.isActive()
                || currentUser.getAuthVersion() != sessionUser.getAuthVersion()) {
            session.invalidate();
            TenantContext.clear();
            if (path.startsWith("/api/")) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Phiên đăng nhập không còn hiệu lực.");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?sessionChanged=1");
            }
            return;
        }

        session.setAttribute("currentUser", currentUser);
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            boolean allowed = path.equals("/super-admin")
                    || path.equals("/users")
                    || path.equals("/logout")
                    || path.equals("/common/sidebar")
                    || path.equals("/api/session");
            if (!allowed) {
                TenantContext.clear();
                if (path.startsWith("/api/")) {
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Super Admin không truy cập dữ liệu nghiệp vụ cửa hàng.");
                } else {
                    httpResponse.sendRedirect(
                            httpRequest.getContextPath() + "/super-admin");
                }
                return;
            }
        } else if (path.equals("/super-admin")) {
            TenantContext.clear();
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chức năng này chỉ dành cho Super Admin.");
            return;
        }
        if (ADMIN.contains(path) && currentUser.getRole() != UserRole.ADMIN) {
            TenantContext.clear();
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chức năng này chỉ dành cho quản trị viên.");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void requireLogin(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        if (path.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập.");
            return;
        }
        request.getSession().setAttribute("loginNext", request.getRequestURI());
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
