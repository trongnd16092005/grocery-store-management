package com.retail.retailstoremanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.retailstoremanagement.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@WebServlet("/api/payments/payos/webhook")
public class PayOsWebhookServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final PaymentService service = new PaymentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String body = new String(
                request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            JsonNode root = JSON.readTree(body);
            PaymentService.WebhookResult result = service.processWebhook(root, body);
            JSON.writeValue(response.getWriter(), Map.of(
                    "success", true,
                    "processed", result.isProcessed(),
                    "message", result.getMessage()));
        } catch (SecurityException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JSON.writeValue(response.getWriter(), Map.of(
                    "success", false, "message", exception.getMessage()));
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSON.writeValue(response.getWriter(), Map.of(
                    "success", false,
                    "message", exception.getMessage() == null
                            ? "Webhook không hợp lệ." : exception.getMessage()));
        }
    }
}
