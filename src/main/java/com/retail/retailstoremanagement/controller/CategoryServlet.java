package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.Category;
import com.retail.retailstoremanagement.service.CategoryService;
import com.retail.retailstoremanagement.service.ValidationException;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/categories")
public class CategoryServlet extends HttpServlet {
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String keyword = RequestUtils.text(request, "q");
            request.setAttribute("categories", categoryService.search(keyword));
            request.setAttribute("keyword", keyword);
            request.setAttribute("flashSuccess", RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError", RequestUtils.consumeFlash(request, "flashError"));

            Long editId = RequestUtils.optionalLong(request, "editId");
            if (editId != null) {
                request.setAttribute("editingCategory", categoryService.findById(editId));
            }
            request.getRequestDispatcher("/WEB-INF/views/categories.jsp").forward(request, response);
        } catch (SQLException | NumberFormatException exception) {
            throw new ServletException("Không thể tải danh mục.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = RequestUtils.text(request, "action");
        try {
            if ("delete".equals(action)) {
                categoryService.delete(RequestUtils.requiredLong(request, "id"));
                RequestUtils.flash(request, "flashSuccess", "Đã xóa danh mục.");
            } else {
                Category category = new Category();
                category.setId(RequestUtils.optionalLong(request, "id"));
                category.setName(RequestUtils.text(request, "name"));
                category.setDescription(RequestUtils.text(request, "description"));
                category.setActive(true);
                boolean creating = category.getId() == null;
                categoryService.save(category);
                RequestUtils.flash(
                        request, "flashSuccess",
                        creating ? "Đã thêm danh mục." : "Đã cập nhật danh mục."
                );
            }
        } catch (ValidationException | NumberFormatException exception) {
            RequestUtils.flash(request, "flashError", exception.getMessage());
        } catch (SQLException exception) {
            throw new ServletException("Không thể lưu danh mục.", exception);
        }
        response.sendRedirect(request.getContextPath() + "/categories");
    }
}
