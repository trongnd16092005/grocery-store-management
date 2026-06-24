package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.Supplier;
import com.retail.retailstoremanagement.service.SupplierService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/suppliers")
public class SupplierServlet extends HttpServlet {
    private final SupplierService service = new SupplierService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String keyword = RequestUtils.text(request, "q");
            boolean includeInactive = "true".equals(request.getParameter("includeInactive"));
            request.setAttribute("suppliers", service.search(keyword, includeInactive));
            request.setAttribute("keyword", keyword);
            request.setAttribute("includeInactive", includeInactive);
            Long id = RequestUtils.optionalLong(request, "editId");
            if (id != null) request.setAttribute("editingSupplier", service.find(id));
            request.setAttribute("flashSuccess",
                    RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError",
                    RequestUtils.consumeFlash(request, "flashError"));
            request.getRequestDispatcher("/WEB-INF/views/suppliers.jsp")
                    .forward(request, response);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String action = RequestUtils.text(request, "action");
            if ("lock".equals(action) || "unlock".equals(action)) {
                boolean active = "unlock".equals(action);
                service.setActive(RequestUtils.requiredLong(request, "id"), active);
                RequestUtils.flash(request, "flashSuccess",
                        active ? "Đã mở khóa nhà cung cấp." : "Đã khóa nhà cung cấp.");
            } else {
                Supplier supplier = new Supplier();
                supplier.setId(RequestUtils.optionalLong(request, "id"));
                supplier.setName(RequestUtils.text(request, "name"));
                supplier.setPhone(RequestUtils.text(request, "phone"));
                supplier.setEmail(RequestUtils.text(request, "email"));
                supplier.setAddress(RequestUtils.text(request, "address"));
                service.save(supplier);
                RequestUtils.flash(request, "flashSuccess", "Đã lưu nhà cung cấp.");
            }
        } catch (Exception exception) {
            String message = exception.getMessage();
            RequestUtils.flash(request, "flashError",
                    message == null ? "Không thể cập nhật nhà cung cấp." : message);
        }
        response.sendRedirect(request.getContextPath() + "/suppliers");
    }
}
