package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.service.StoreService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/store")
public class StoreServlet extends HttpServlet {
    private final StoreService service = new StoreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("store", service.findCurrent());
            request.setAttribute("flashSuccess",
                    RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError",
                    RequestUtils.consumeFlash(request, "flashError"));
            request.getRequestDispatcher("/WEB-INF/views/store.jsp")
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
            if ("payos".equals(action)) {
                service.updatePayOs(
                        "on".equalsIgnoreCase(request.getParameter("payosEnabled")),
                        RequestUtils.text(request, "payosClientId"),
                        RequestUtils.text(request, "payosApiKey"),
                        RequestUtils.text(request, "payosChecksumKey")
                );
                RequestUtils.flash(request, "flashSuccess",
                        "Đã cập nhật cấu hình QR payOS.");
            } else {
                Store store = service.update(
                        RequestUtils.text(request, "name"),
                        RequestUtils.text(request, "phone"),
                        RequestUtils.text(request, "address")
                );
                AppUser user = (AppUser) request.getSession().getAttribute("currentUser");
                if (user != null) user.setStoreName(store.getName());
                RequestUtils.flash(request, "flashSuccess",
                        "Đã cập nhật thông tin cửa hàng.");
            }
        } catch (Exception exception) {
            String message = exception.getMessage();
            RequestUtils.flash(request, "flashError",
                    message == null ? "Không thể cập nhật thông tin cửa hàng." : message);
        }
        response.sendRedirect(request.getContextPath() + "/store");
    }
}
