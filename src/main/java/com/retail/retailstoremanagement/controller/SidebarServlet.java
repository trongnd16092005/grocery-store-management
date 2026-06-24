package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/common/sidebar")
public class SidebarServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AppUser user = (AppUser) request.getSession().getAttribute("currentUser");
        String context = request.getContextPath();
        String storeName = user == null ? "Cửa hàng" : escape(user.getStoreName());
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder html = new StringBuilder(
                "<div class=\"sidebar\"><div class=\"logo\"><i class=\"fa-solid fa-store me-2\"></i>")
                .append(storeName).append("</div><ul class=\"sidebar-nav\">");
        link(html, context + "/dashboard", "fa-chart-pie", "Dashboard");
        if (user != null && user.getRole() == UserRole.ADMIN) {
            link(html, context + "/store", "fa-store", "Thông tin cửa hàng");
            link(html, context + "/products", "fa-box-open", "Sản phẩm");
            link(html, context + "/categories", "fa-tags", "Danh mục");
            link(html, context + "/suppliers", "fa-truck-field", "Nhà cung cấp");
            link(html, context + "/inventory", "fa-warehouse", "Quản lý kho");
            link(html, context + "/purchase-orders", "fa-file-circle-plus", "Phiếu nhập hàng");
            link(html, context + "/discount-codes", "fa-ticket", "Mã giảm giá");
        }
        link(html, context + "/customers", "fa-users", "Khách hàng");
        link(html, context + "/sale", "fa-cash-register", "Bán hàng");
        link(html, context + "/invoices", "fa-file-invoice", "Hóa đơn");
        link(html, context + "/users", "fa-user-shield",
                user != null && user.getRole() == UserRole.ADMIN ? "Tài khoản" : "Đổi mật khẩu");
        html.append("<li class=\"mt-auto\"><a href=\"").append(context)
                .append("/logout\" class=\"text-danger-hover\"><i class=\"fa-solid fa-right-from-bracket me-2\"></i>Đăng xuất</a></li></ul></div>");
        response.getWriter().print(html);
    }

    private void link(StringBuilder html, String url, String icon, String text) {
        html.append("<li><a href=\"").append(url).append("\"><i class=\"fa-solid ")
                .append(icon).append(" me-2\"></i>").append(text).append("</a></li>");
    }

    private String escape(String value) {
        if (value == null) return "Cửa hàng";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
