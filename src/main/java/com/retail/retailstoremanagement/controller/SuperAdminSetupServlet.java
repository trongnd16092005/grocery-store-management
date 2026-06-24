package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.service.SuperAdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/super-admin/setup")
public class SuperAdminSetupServlet extends HttpServlet {
    private final SuperAdminService service = new SuperAdminService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!service.needsSetup()) {
                response.sendRedirect(request.getContextPath() + "/login?superAdminReady=1");
                return;
            }
            request.getRequestDispatcher("/WEB-INF/views/super-admin-setup.jsp")
                    .forward(request, response);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String password = request.getParameter("password");
            if (password == null || !password.equals(request.getParameter("confirmPassword"))) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            }
            AppUser user = service.setupFirst(
                    request.getParameter("setupKey"),
                    request.getParameter("username"),
                    password,
                    request.getParameter("fullName")
            );
            request.getSession(true);
            request.changeSessionId();
            request.getSession().setAttribute("currentUser", user);
            response.sendRedirect(request.getContextPath() + "/super-admin");
        } catch (Exception exception) {
            request.setAttribute("error", exception.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/super-admin-setup.jsp")
                    .forward(request, response);
        }
    }
}
