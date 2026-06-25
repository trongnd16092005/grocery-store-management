package com.retail.retailstoremanagement.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/payment/return", "/payment/cancel"})
public class PaymentRedirectServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String result = request.getServletPath().endsWith("/cancel")
                ? "cancelled" : "returned";
        response.sendRedirect(request.getContextPath() + "/sale?payment=" + result);
    }
}
