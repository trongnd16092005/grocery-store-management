package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.service.SuperAdminService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/super-admin")
public class SuperAdminServlet extends HttpServlet {
    private final SuperAdminService service = new SuperAdminService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Store> stores = service.findStores();
            request.setAttribute("stores", stores);
            request.setAttribute("totalStores", stores.size());
            request.setAttribute("activeStores",
                    stores.stream().filter(Store::isActive).count());
            request.setAttribute("inactiveStores",
                    stores.stream().filter(store -> !store.isActive()).count());
            request.setAttribute("flashSuccess",
                    RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError",
                    RequestUtils.consumeFlash(request, "flashError"));
            request.getRequestDispatcher("/WEB-INF/views/super-admin.jsp")
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
            if (!"lock".equals(action) && !"unlock".equals(action)) {
                throw new IllegalArgumentException("Thao tác cửa hàng không hợp lệ.");
            }
            boolean active = "unlock".equals(action);
            service.setStoreActive(RequestUtils.requiredLong(request, "id"), active);
            RequestUtils.flash(request, "flashSuccess",
                    active ? "Đã mở khóa cửa hàng." : "Đã khóa cửa hàng.");
        } catch (Exception exception) {
            RequestUtils.flash(request, "flashError",
                    exception.getMessage() == null
                            ? "Không thể cập nhật cửa hàng." : exception.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/super-admin");
    }
}
