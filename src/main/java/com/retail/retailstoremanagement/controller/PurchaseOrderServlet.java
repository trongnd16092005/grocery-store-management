package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcSupplierDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.PurchaseOrderDetail;
import com.retail.retailstoremanagement.service.ProductService;
import com.retail.retailstoremanagement.service.PurchaseOrderService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/purchase-orders")
public class PurchaseOrderServlet extends HttpServlet {
    private final PurchaseOrderService service = new PurchaseOrderService();
    private final ProductService productService = new ProductService();
    private final JdbcSupplierDao supplierDao = new JdbcSupplierDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String keyword = RequestUtils.text(request, "q");
            String status = RequestUtils.text(request, "status");
            int page = Math.max(1, RequestUtils.integer(request, "page", 1));
            request.setAttribute("orders", service.findAll(keyword, status, page));
            request.setAttribute("products", productService.findAll());
            request.setAttribute("suppliers", supplierDao.findAll());
            request.setAttribute("keyword", keyword);
            request.setAttribute("selectedStatus", status);
            request.setAttribute("page", page);
            Long id = RequestUtils.optionalLong(request, "id");
            if (id != null) request.setAttribute("selectedOrder", service.findById(id));
            request.setAttribute("flashSuccess",
                    RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError",
                    RequestUtils.consumeFlash(request, "flashError"));
            request.getRequestDispatcher("/WEB-INF/views/purchase-orders.jsp")
                    .forward(request, response);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AppUser user = (AppUser) request.getSession().getAttribute("currentUser");
        try {
            String action = RequestUtils.text(request, "action");
            if ("create".equals(action)) {
                service.createDraft(
                        RequestUtils.requiredLong(request, "supplierId"),
                        RequestUtils.text(request, "note"),
                        user == null ? null : user.getId(),
                        parseDetails(request)
                );
                RequestUtils.flash(request, "flashSuccess",
                        "Đã tạo phiếu nhập nháp. Hãy kiểm tra và hoàn thành để cộng tồn.");
            } else if ("complete".equals(action)) {
                service.complete(RequestUtils.requiredLong(request, "id"),
                        user == null ? null : user.getId());
                RequestUtils.flash(request, "flashSuccess",
                        "Đã hoàn thành phiếu nhập và cập nhật tồn kho.");
            } else if ("cancel".equals(action)) {
                service.cancel(RequestUtils.requiredLong(request, "id"),
                        user == null ? null : user.getId());
                RequestUtils.flash(request, "flashSuccess",
                        "Đã hủy phiếu nhập và hoàn tác tồn kho nếu cần.");
            } else {
                throw new IllegalArgumentException("Thao tác phiếu nhập không hợp lệ.");
            }
        } catch (Exception exception) {
            String message = exception.getMessage();
            RequestUtils.flash(request, "flashError",
                    message == null ? "Không thể cập nhật phiếu nhập." : message);
        }
        response.sendRedirect(request.getContextPath() + "/purchase-orders");
    }

    private List<PurchaseOrderDetail> parseDetails(HttpServletRequest request) {
        String[] productIds = request.getParameterValues("productId");
        String[] quantities = request.getParameterValues("quantity");
        String[] costs = request.getParameterValues("unitCost");
        if (productIds == null || quantities == null || costs == null
                || productIds.length != quantities.length
                || productIds.length != costs.length) {
            throw new IllegalArgumentException("Danh sách sản phẩm nhập không hợp lệ.");
        }
        List<PurchaseOrderDetail> details = new ArrayList<>();
        for (int index = 0; index < productIds.length; index++) {
            if (productIds[index] == null || productIds[index].isBlank()) continue;
            PurchaseOrderDetail detail = new PurchaseOrderDetail();
            detail.setProductId(Long.valueOf(productIds[index]));
            detail.setQuantity(Integer.parseInt(quantities[index]));
            detail.setUnitCost(new BigDecimal(costs[index]));
            details.add(detail);
        }
        return details;
    }
}
