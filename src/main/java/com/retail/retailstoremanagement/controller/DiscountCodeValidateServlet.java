package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.service.DiscountCodeService;
import com.retail.retailstoremanagement.service.CustomerService;
import com.retail.retailstoremanagement.model.Customer;
import com.retail.retailstoremanagement.model.CustomerType;
import com.retail.retailstoremanagement.util.JsonUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet("/api/discount-codes/validate")
public class DiscountCodeValidateServlet extends HttpServlet {
    private final DiscountCodeService service = new DiscountCodeService();
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            BigDecimal subtotal = new BigDecimal(request.getParameter("subtotal"));
            CustomerType customerType = null;
            String customerCode = request.getParameter("customerCode");
            if (customerCode != null && !customerCode.isBlank()) {
                Optional<Customer> customer = customerService.findByCode(customerCode);
                if (customer.isPresent()) customerType = customer.get().getCustomerType();
            }
            Map<String, BigDecimal> lineTotals = new LinkedHashMap<>();
            String[] productCodes = request.getParameterValues("productCode");
            String[] totals = request.getParameterValues("lineTotal");
            if (productCodes != null && totals != null) {
                for (int i = 0; i < productCodes.length && i < totals.length; i++) {
                    lineTotals.merge(productCodes[i].toUpperCase(),
                            new BigDecimal(totals[i]), BigDecimal::add);
                }
            }
            DiscountCodeService.DiscountPreview result =
                    service.previewCart(request.getParameter("code"), subtotal,
                            customerType, lineTotals);
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
