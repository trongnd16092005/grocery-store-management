package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.SupplierDao;
import com.retail.retailstoremanagement.dao.impl.JdbcSupplierDao;
import com.retail.retailstoremanagement.model.Product;
import com.retail.retailstoremanagement.service.CategoryService;
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

@WebServlet("/products")
public class ProductServlet extends HttpServlet {
    private static final int PAGE_SIZE = 8;
    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final SupplierDao supplierDao = new JdbcSupplierDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String keyword = RequestUtils.text(request, "q");
            Long categoryId = RequestUtils.optionalLong(request, "categoryId");
            String stockStatus = RequestUtils.text(request, "stockStatus");
            int page = Math.max(1, RequestUtils.integer(request, "page", 1));
            long total = productService.count(keyword, categoryId, stockStatus);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
            page = Math.min(page, totalPages);

            request.setAttribute("products", productService.search(
                    keyword, categoryId, stockStatus, page, PAGE_SIZE
            ));
            request.setAttribute("categories", categoryService.findAll());
            request.setAttribute("suppliers", supplierDao.findAll());
            request.setAttribute("keyword", keyword);
            request.setAttribute("selectedCategoryId", categoryId);
            request.setAttribute("stockStatus", stockStatus);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("total", total);
            request.setAttribute("flashSuccess", RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError", RequestUtils.consumeFlash(request, "flashError"));

            Long editId = RequestUtils.optionalLong(request, "editId");
            if (editId != null) {
                request.setAttribute("editingProduct", productService.findById(editId));
            }
            request.getRequestDispatcher("/WEB-INF/views/products.jsp").forward(request, response);
        } catch (SQLException | NumberFormatException exception) {
            throw new ServletException("Không thể tải sản phẩm.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = RequestUtils.text(request, "action");
        try {
            if ("delete".equals(action)) {
                productService.delete(RequestUtils.requiredLong(request, "id"));
                RequestUtils.flash(request, "flashSuccess", "Đã ngừng kinh doanh sản phẩm.");
            } else {
                Product product = new Product();
                product.setId(RequestUtils.optionalLong(request, "id"));
                product.setBarcode(RequestUtils.text(request, "barcode"));
                product.setName(RequestUtils.text(request, "name"));
                product.setCategoryId(RequestUtils.optionalLong(request, "categoryId"));
                product.setSupplierId(RequestUtils.optionalLong(request, "supplierId"));
                product.setCostPrice(RequestUtils.decimal(request, "costPrice"));
                product.setSellingPrice(RequestUtils.decimal(request, "sellingPrice"));
                product.setMinimumStock(RequestUtils.integer(request, "minimumStock", 0));
                product.setUnit(RequestUtils.text(request, "unit"));
                product.setActive(true);
                boolean creating = product.getId() == null;
                productService.save(product);
                RequestUtils.flash(
                        request, "flashSuccess",
                        creating ? "Đã thêm sản phẩm. Tồn đầu kỳ mặc định là 0."
                                : "Đã cập nhật sản phẩm."
                );
            }
        } catch (ValidationException | NumberFormatException exception) {
            RequestUtils.flash(request, "flashError", exception.getMessage());
        } catch (SQLException exception) {
            throw new ServletException("Không thể lưu sản phẩm.", exception);
        }
        response.sendRedirect(request.getContextPath() + "/products");
    }
}
