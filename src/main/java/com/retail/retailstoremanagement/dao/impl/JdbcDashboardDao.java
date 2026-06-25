package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.DashboardDao;
import com.retail.retailstoremanagement.model.DashboardStats;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.model.InvoiceStatus;
import com.retail.retailstoremanagement.model.PaymentMethod;
import com.retail.retailstoremanagement.model.Product;
import com.retail.retailstoremanagement.model.RevenuePoint;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JdbcDashboardDao implements DashboardDao {
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("dd/MM");

    @Override
    public DashboardStats load() throws SQLException {
        DashboardStats stats = new DashboardStats();
        try (Connection connection = DatabaseConnection.getConnection()) {
            loadTotals(connection, stats);
            loadRevenueChart(connection, stats);
            loadPaymentMix(connection, stats);
            loadRecentInvoices(connection, stats);
            loadLowStockProducts(connection, stats);
        }
        return stats;
    }

    private void loadTotals(Connection connection, DashboardStats stats)
            throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM products WHERE active) product_count,"
                + "(SELECT COUNT(*) FROM customers WHERE active) customer_count,"
                + "(SELECT COUNT(*) FROM low_stock_products) low_stock_count,"
                + "(SELECT COUNT(*) FROM invoices WHERE status='PAID' "
                + "AND created_at::date=CURRENT_DATE) invoice_count,"
                + "(SELECT COALESCE(SUM(total_amount),0) FROM invoices "
                + "WHERE status='PAID' AND created_at::date=CURRENT_DATE) revenue";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            stats.setProductCount(resultSet.getLong("product_count"));
            stats.setCustomerCount(resultSet.getLong("customer_count"));
            stats.setLowStockCount(resultSet.getLong("low_stock_count"));
            stats.setTodayInvoiceCount(resultSet.getLong("invoice_count"));
            stats.setTodayRevenue(resultSet.getBigDecimal("revenue"));
        }
    }

    private void loadRevenueChart(Connection connection, DashboardStats stats)
            throws SQLException {
        String sql = "WITH days AS ("
                + "SELECT generate_series(CURRENT_DATE - INTERVAL '6 days',"
                + "CURRENT_DATE, INTERVAL '1 day')::date sale_date"
                + ") SELECT days.sale_date,"
                + "COALESCE(SUM(i.total_amount),0) revenue "
                + "FROM days LEFT JOIN invoices i "
                + "ON i.created_at::date=days.sale_date AND i.status='PAID' "
                + "GROUP BY days.sale_date ORDER BY days.sale_date";

        List<RevenuePoint> points = new ArrayList<>();
        BigDecimal maximum = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                LocalDate date = resultSet.getObject("sale_date", LocalDate.class);
                BigDecimal revenue = resultSet.getBigDecimal("revenue");

                RevenuePoint point = new RevenuePoint();
                point.setDate(date);
                point.setLabel(date.format(DAY_LABEL));
                point.setRevenue(revenue);
                points.add(point);

                maximum = maximum.max(revenue);
                total = total.add(revenue);
            }
        }

        for (RevenuePoint point : points) {
            point.setChartPercent(chartPercent(point.getRevenue(), maximum));
        }
        stats.setSevenDayRevenue(total);
        stats.setSevenDayRevenuePoints(points);
    }

    private void loadPaymentMix(Connection connection, DashboardStats stats)
            throws SQLException {
        String sql = "SELECT "
                + "COUNT(*) FILTER (WHERE payment_method='CASH') cash_count,"
                + "COUNT(*) FILTER (WHERE payment_method='QR') qr_count "
                + "FROM invoices WHERE status='PAID' "
                + "AND created_at >= CURRENT_DATE - INTERVAL '29 days'";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            long cash = resultSet.getLong("cash_count");
            long qr = resultSet.getLong("qr_count");
            long total = cash + qr;

            stats.setCashPaymentCount(cash);
            stats.setQrPaymentCount(qr);
            stats.setCashPaymentPercent(percent(cash, total));
            stats.setQrPaymentPercent(total == 0
                    ? 0 : 100 - stats.getCashPaymentPercent());
        }
    }

    private void loadRecentInvoices(Connection connection, DashboardStats stats)
            throws SQLException {
        String sql = "SELECT i.*,c.full_name customer_name,"
                + "u.full_name cashier_name FROM invoices i "
                + "LEFT JOIN customers c ON c.id=i.customer_id "
                + "LEFT JOIN app_users u ON u.id=i.cashier_id "
                + "ORDER BY i.created_at DESC LIMIT 5";
        List<Invoice> invoices = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                Invoice invoice = new Invoice();
                invoice.setId(resultSet.getLong("id"));
                invoice.setCode(resultSet.getString("code"));
                invoice.setCustomerName(resultSet.getString("customer_name"));
                invoice.setCashierName(resultSet.getString("cashier_name"));
                invoice.setPaymentMethod(PaymentMethod.valueOf(
                        resultSet.getString("payment_method")));
                invoice.setStatus(InvoiceStatus.valueOf(
                        resultSet.getString("status")));
                invoice.setTotalAmount(resultSet.getBigDecimal("total_amount"));
                invoice.setCreatedAt(resultSet.getObject(
                        "created_at", OffsetDateTime.class));
                invoices.add(invoice);
            }
        }
        stats.setRecentInvoices(invoices);
    }

    private void loadLowStockProducts(Connection connection, DashboardStats stats)
            throws SQLException {
        String sql = "SELECT code,name,category_name,stock_quantity,minimum_stock "
                + "FROM low_stock_products "
                + "ORDER BY stock_quantity ASC, minimum_stock DESC, name ASC "
                + "LIMIT 5";
        List<Product> products = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                Product product = new Product();
                product.setCode(resultSet.getString("code"));
                product.setName(resultSet.getString("name"));
                product.setCategoryName(resultSet.getString("category_name"));
                product.setStockQuantity(resultSet.getInt("stock_quantity"));
                product.setMinimumStock(resultSet.getInt("minimum_stock"));
                products.add(product);
            }
        }
        stats.setLowStockProducts(products);
    }

    private int chartPercent(BigDecimal value, BigDecimal maximum) {
        if (value.signum() <= 0 || maximum.signum() <= 0) {
            return 0;
        }
        int percentage = value.multiply(BigDecimal.valueOf(100))
                .divide(maximum, 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(6, percentage);
    }

    private int percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(value * 100L)
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
