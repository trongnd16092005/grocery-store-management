package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.SupplierDao;
import com.retail.retailstoremanagement.dao.impl.JdbcSupplierDao;
import com.retail.retailstoremanagement.model.Product;
import com.retail.retailstoremanagement.service.InventoryService;
import com.retail.retailstoremanagement.service.ProductService;
import com.retail.retailstoremanagement.service.ValidationException;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/inventory")
public class InventoryServlet extends HttpServlet {
    private final ProductService productService = new ProductService();
    private final InventoryService inventoryService = new InventoryService();
    private final SupplierDao supplierDao = new JdbcSupplierDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Product> products = productService.findAll();
            long totalStock = products.stream().mapToLong(Product::getStockQuantity).sum();
            long lowStock = products.stream().filter(Product::isLowStock).count();
            long outOfStock = products.stream().filter(Product::isOutOfStock).count();

            request.setAttribute("products", products);
            request.setAttribute("suppliers", supplierDao.findAll());
            request.setAttribute("transactions", inventoryService.findRecentTransactions(30));
            request.setAttribute("totalStock", totalStock);
            request.setAttribute("lowStock", lowStock);
            request.setAttribute("outOfStock", outOfStock);
            request.setAttribute("flashSuccess", RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError", RequestUtils.consumeFlash(request, "flashError"));
            request.getRequestDispatcher("/WEB-INF/views/inventory.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Không thể tải dữ liệu kho.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = RequestUtils.text(request, "action");
        try {
            if ("import".equals(action)) {
                inventoryService.importStock(
                        RequestUtils.requiredLong(request, "productId"),
                        RequestUtils.integer(request, "quantity", 0),
                        RequestUtils.decimal(request, "unitCost"),
                        RequestUtils.requiredLong(request, "supplierId"),
                        RequestUtils.text(request, "note"),
                        null
                );
                RequestUtils.flash(request, "flashSuccess", "Đã nhập hàng và cập nhật tồn kho.");
            } else if ("adjust".equals(action)) {
                inventoryService.adjustStock(
                        RequestUtils.requiredLong(request, "productId"),
                        RequestUtils.integer(request, "newStock", -1),
                        RequestUtils.text(request, "reason"),
                        null
                );
                RequestUtils.flash(request, "flashSuccess", "Đã điều chỉnh tồn kho.");
            } else {
                throw new ValidationException("Thao tác kho không hợp lệ.");
            }
        } catch (ValidationException | NumberFormatException exception) {
            RequestUtils.flash(request, "flashError", exception.getMessage());
        } catch (SQLException exception) {
            RequestUtils.flash(request, "flashError", "Không thể cập nhật kho: " + exception.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/inventory");
    }
}
