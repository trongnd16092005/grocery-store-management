package com.retail.retailstoremanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.retailstoremanagement.model.PaymentTransaction;
import com.retail.retailstoremanagement.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/payments/status")
public class PaymentStatusServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final PaymentService service = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            PaymentTransaction payment = service.getStatus(
                    Long.parseLong(request.getParameter("invoiceId")));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("invoiceId", payment.getInvoiceId());
            result.put("code", payment.getInvoiceCode());
            result.put("status", payment.getStatus().name());
            result.put("total", payment.getAmount());
            result.put("paidAt", payment.getPaidAt() == null
                    ? null : payment.getPaidAt().toString());
            result.put("message", payment.getFailureReason());
            JSON.writeValue(response.getWriter(), result);
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSON.writeValue(response.getWriter(), Map.of(
                    "success", false,
                    "message", exception.getMessage() == null
                            ? "Không thể kiểm tra giao dịch." : exception.getMessage()));
        }
    }
}
