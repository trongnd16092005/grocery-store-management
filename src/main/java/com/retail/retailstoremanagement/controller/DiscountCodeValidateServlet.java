package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.service.DiscountCodeService;
import com.retail.retailstoremanagement.util.JsonUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/api/discount-codes/validate")
public class DiscountCodeValidateServlet extends HttpServlet {
    private final DiscountCodeService service = new DiscountCodeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            BigDecimal subtotal = new BigDecimal(request.getParameter("subtotal"));
            DiscountCodeService.DiscountPreview result =
                    service.preview(request.getParameter("code"), subtotal);
            response.getWriter().printf(
                    "{\"valid\":true,\"code\":\"%s\",\"name\":\"%s\","
                            + "\"discount\":%s,\"total\":%s}",
                    JsonUtils.escape(result.getCode()), JsonUtils.escape(result.getName()),
                    result.getDiscount(), result.getTotal()
            );
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().printf(
                    "{\"valid\":false,\"message\":\"%s\"}",
                    JsonUtils.escape(exception.getMessage())
            );
        }
    }
}
