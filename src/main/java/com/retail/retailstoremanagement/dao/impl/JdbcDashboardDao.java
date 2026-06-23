package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.DashboardDao;
import com.retail.retailstoremanagement.model.*;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public class JdbcDashboardDao implements DashboardDao {
    @Override public DashboardStats load() throws SQLException {
        DashboardStats stats = new DashboardStats();
        try (Connection c = DatabaseConnection.getConnection()) {
            String totals = "SELECT (SELECT COUNT(*) FROM products WHERE active) product_count, "
                    + "(SELECT COUNT(*) FROM customers WHERE active) customer_count, "
                    + "(SELECT COUNT(*) FROM low_stock_products) low_stock_count, "
                    + "(SELECT COUNT(*) FROM invoices WHERE status='PAID' AND created_at::date=CURRENT_DATE) invoice_count, "
                    + "(SELECT COALESCE(SUM(total_amount),0) FROM invoices WHERE status='PAID' AND created_at::date=CURRENT_DATE) revenue";
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(totals)) {
                r.next(); stats.setProductCount(r.getLong("product_count"));
                stats.setCustomerCount(r.getLong("customer_count"));
                stats.setLowStockCount(r.getLong("low_stock_count"));
                stats.setTodayInvoiceCount(r.getLong("invoice_count"));
                stats.setTodayRevenue(r.getBigDecimal("revenue"));
            }
            String recent = "SELECT i.*, c.full_name customer_name, u.full_name cashier_name FROM invoices i "
                    + "LEFT JOIN customers c ON c.id=i.customer_id LEFT JOIN app_users u ON u.id=i.cashier_id "
                    + "ORDER BY i.created_at DESC LIMIT 5";
            ArrayList<Invoice> invoices = new ArrayList<>();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(recent)) {
                while (r.next()) {
                    Invoice i = new Invoice(); i.setId(r.getLong("id")); i.setCode(r.getString("code"));
                    i.setCustomerName(r.getString("customer_name")); i.setCashierName(r.getString("cashier_name"));
                    i.setPaymentMethod(PaymentMethod.valueOf(r.getString("payment_method")));
                    i.setStatus(InvoiceStatus.valueOf(r.getString("status"))); i.setTotalAmount(r.getBigDecimal("total_amount"));
                    i.setCreatedAt(r.getObject("created_at", OffsetDateTime.class)); invoices.add(i);
                }
            }
            stats.setRecentInvoices(invoices);
        }
        return stats;
    }
}
