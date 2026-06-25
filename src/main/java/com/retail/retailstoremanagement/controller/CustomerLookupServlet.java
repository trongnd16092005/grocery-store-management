package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.Customer;
import com.retail.retailstoremanagement.service.CustomerService;
import com.retail.retailstoremanagement.util.JsonUtils;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/api/customers/lookup")
public class CustomerLookupServlet extends HttpServlet {
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            Optional<Customer> customer = customerService.findByCode(
                    RequestUtils.text(request, "code")
            );
            if (customer.isEmpty()) {
                response.getWriter().write("{\"found\":false}");
                return;
            }
            Customer value = customer.get();
            response.getWriter().printf(
                    "{\"found\":true,\"code\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\","
                            + "\"type\":\"%s\",\"points\":%d,"
                            + "\"lifetimePoints\":%d,\"totalSpent\":%s}",
                    JsonUtils.escape(value.getCode()),
                    JsonUtils.escape(value.getFullName()),
                    JsonUtils.escape(value.getPhone()),
                    value.getCustomerType().name(),
                    value.getLoyaltyPoints(),
                    value.getLifetimeLoyaltyPoints(),
                    value.getTotalSpent()
            );
        } catch (SQLException exception) {
            throw new ServletException("Không thể tra cứu khách hàng.", exception);
        }
    }
}
