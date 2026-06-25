package com.retail.retailstoremanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.retailstoremanagement.model.PaymentTransaction;
import com.retail.retailstoremanagement.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/payments/cancel")
public class PaymentCancelServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final PaymentService service = new PaymentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            PaymentTransaction payment = service.cancel(
                    Long.parseLong(request.getParameter("invoiceId")));
            JSON.writeValue(response.getWriter(), Map.of(
                    "success", true,
                    "status", payment.getStatus().name()));
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSON.writeValue(response.getWriter(), Map.of(
                    "success", false,
                    "message", exception.getMessage() == null
                            ? "Không thể hủy giao dịch." : exception.getMessage()));
        }
    }
}
