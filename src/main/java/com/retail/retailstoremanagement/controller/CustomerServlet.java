package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.Customer;
import com.retail.retailstoremanagement.model.CustomerType;
import com.retail.retailstoremanagement.model.Gender;
import com.retail.retailstoremanagement.service.CustomerService;
import com.retail.retailstoremanagement.service.ValidationException;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/customers")
public class CustomerServlet extends HttpServlet {
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String keyword = RequestUtils.text(request, "q");
            String customerType = RequestUtils.text(request, "customerType");
            request.setAttribute("customers", customerService.search(keyword, customerType));
            request.setAttribute("keyword", keyword);
            request.setAttribute("selectedCustomerType", customerType);
            request.setAttribute("flashSuccess", RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError", RequestUtils.consumeFlash(request, "flashError"));

            Long editId = RequestUtils.optionalLong(request, "editId");
            if (editId != null) {
                request.setAttribute("editingCustomer", customerService.findById(editId));
            }
            request.getRequestDispatcher("/WEB-INF/views/customers.jsp").forward(request, response);
        } catch (SQLException | NumberFormatException exception) {
            throw new ServletException("Không thể tải khách hàng.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            Customer customer = new Customer();
            customer.setId(RequestUtils.optionalLong(request, "id"));
            customer.setFullName(RequestUtils.text(request, "fullName"));
            customer.setPhone(RequestUtils.text(request, "phone"));
            customer.setEmail(RequestUtils.text(request, "email"));
            String gender = RequestUtils.text(request, "gender");
            customer.setGender(gender.isEmpty() ? null : Gender.valueOf(gender));
            customer.setAddress(RequestUtils.text(request, "address"));
            String type = RequestUtils.text(request, "customerType");
            customer.setCustomerType(
                    type.isEmpty() ? CustomerType.REGULAR : CustomerType.valueOf(type)
            );
            customer.setActive(true);
            boolean creating = customer.getId() == null;
            customerService.save(customer);
            RequestUtils.flash(
                    request, "flashSuccess",
                    creating ? "Đã thêm khách hàng." : "Đã cập nhật khách hàng."
            );
        } catch (ValidationException | IllegalArgumentException exception) {
            RequestUtils.flash(request, "flashError", exception.getMessage());
        } catch (SQLException exception) {
            throw new ServletException("Không thể lưu khách hàng.", exception);
        }
        response.sendRedirect(request.getContextPath() + "/customers");
    }
}
