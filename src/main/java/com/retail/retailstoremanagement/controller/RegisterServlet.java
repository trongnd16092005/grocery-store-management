package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final AuthService service = new AuthService(new JdbcUserDao());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            if (!request.getParameter("password").equals(request.getParameter("confirmPassword"))) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            }
            AppUser user = service.registerStore(
                    request.getParameter("storeCode"), request.getParameter("storeName"),
                    request.getParameter("phone"), request.getParameter("address"),
                    request.getParameter("username"), request.getParameter("password"),
                    request.getParameter("fullName")
            );
            request.getSession(true).setAttribute("currentUser", user);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception exception) {
            request.setAttribute("error", exception.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }
}
