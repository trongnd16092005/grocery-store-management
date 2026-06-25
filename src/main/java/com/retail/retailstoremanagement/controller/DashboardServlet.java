package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcDashboardDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    private final JdbcDashboardDao dashboardDao = new JdbcDashboardDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("stats", dashboardDao.load());
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp")
                    .forward(request, response);
        } catch (Exception exception) {
            throw new ServletException("Không thể tải dashboard.", exception);
        }
    }
}
