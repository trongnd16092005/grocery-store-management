package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final AuthService service = new AuthService(new JdbcUserDao());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("1".equals(request.getParameter("sessionChanged"))) {
            request.setAttribute(
                    "info",
                    "Tài khoản, mật khẩu hoặc trạng thái cửa hàng vừa thay đổi. "
                            + "Vui lòng đăng nhập lại."
            );
        }
        if ("1".equals(request.getParameter("passwordChanged"))) {
            request.setAttribute(
                    "info",
                    "Đã đổi mật khẩu. Vui lòng đăng nhập lại bằng mật khẩu mới."
            );
        }
        request.getRequestDispatcher("/WEB-INF/views/login.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            AppUser user = service.login(
                    request.getParameter("storeCode"),
                    request.getParameter("username"),
                    request.getParameter("password")
            );
            if (user == null) {
                showLoginError(request, response);
                return;
            }

            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("currentUser", user);

            String next = (String) session.getAttribute("loginNext");
            session.removeAttribute("loginNext");

            String defaultTarget = user.getRole() == UserRole.SUPER_ADMIN
                    ? "/super-admin" : "/sale";
            String target = isSafeLocalTarget(request, next)
                    ? next : request.getContextPath() + defaultTarget;
            response.sendRedirect(target);
        } catch (Exception exception) {
            throw new ServletException("Không thể xử lý đăng nhập.", exception);
        }
    }

    private void showLoginError(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute(
                "error",
                "Mã cửa hàng, tên đăng nhập hoặc mật khẩu không đúng."
        );
        request.setAttribute("storeCode", request.getParameter("storeCode"));
        request.setAttribute("username", request.getParameter("username"));
        request.getRequestDispatcher("/WEB-INF/views/login.jsp")
                .forward(request, response);
    }

    private boolean isSafeLocalTarget(HttpServletRequest request, String target) {
        return target != null
                && target.startsWith(request.getContextPath() + "/")
                && !target.startsWith("//");
    }
}
