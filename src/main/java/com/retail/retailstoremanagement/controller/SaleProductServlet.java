package com.retail.retailstoremanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.retailstoremanagement.dao.impl.JdbcProductDao;
import com.retail.retailstoremanagement.model.Product;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/products/sale")
public class SaleProductServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcProductDao productDao = new JdbcProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            List<Map<String, Object>> products = productDao.findAll().stream()
                    .filter(Product::isActive)
                    .map(this::toSaleProduct)
                    .collect(Collectors.toList());
            JSON.writeValue(response.getWriter(), products);
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSON.writeValue(
                    response.getWriter(),
                    Map.of(
                            "message",
                            exception.getMessage() == null
                                    ? "Không thể tải sản phẩm."
                                    : exception.getMessage()
                    )
            );
        }
    }

    private Map<String, Object> toSaleProduct(Product product) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", product.getCode());
        result.put("name", product.getName());
        result.put("cat", product.getCategoryName());
        result.put("price", product.getSellingPrice());
        result.put("stock", product.getStockQuantity());
        return result;
    }
}
