package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcPaymentDao;
import com.retail.retailstoremanagement.dao.impl.JdbcInvoiceDao;
import com.retail.retailstoremanagement.model.PaymentStatus;
import com.retail.retailstoremanagement.model.PaymentTransaction;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TestTenantContext;
import com.retail.retailstoremanagement.util.TenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.Map;

/** Manual DB integration test for QR stock hold and release. */
public final class PaymentWorkflowSmokeTest {
    private PaymentWorkflowSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        TestTenantContext.activateDefaultStore();
        JdbcPaymentDao dao = new JdbcPaymentDao();
        long userId = findAdmin();
        ProductSnapshot product = findProduct();
        PaymentTransaction payment = null;
        PaymentTransaction paidPayment = null;
        try {
            payment = dao.createPending(
                    Map.of(product.code, 1), null, null, userId,
                    OffsetDateTime.now().plusMinutes(10));
            if (stock(product.id) != product.stock - 1) {
                throw new IllegalStateException("QR payment did not hold stock.");
            }
            payment = dao.release(payment.getOrderCode(), PaymentStatus.CANCELLED,
                    "Smoke test release");
            if (payment.getStatus() != PaymentStatus.CANCELLED
                    || stock(product.id) != product.stock) {
                throw new IllegalStateException("QR cancellation did not restore stock.");
            }

            paidPayment = dao.createPending(
                    Map.of(product.code, 1), null, null, userId,
                    OffsetDateTime.now().plusMinutes(10));
            String reference = "SMOKE-" + System.currentTimeMillis();
            dao.markPaid(paidPayment.getOrderCode(), paidPayment.getAmount(),
                    reference, "{}");
            dao.markPaid(paidPayment.getOrderCode(), paidPayment.getAmount(),
                    reference, "{}");
            if (stock(product.id) != product.stock - 1) {
                throw new IllegalStateException("Duplicate webhook changed stock twice.");
            }
            new JdbcInvoiceDao().cancel(paidPayment.getInvoiceId(), userId);
            if (stock(product.id) != product.stock) {
                throw new IllegalStateException("Paid QR cancellation did not restore stock.");
            }
            System.out.printf(
                    "paymentWorkflowSmoke=true, orderCode=%d, holdRelease=true, idempotent=true%n",
                    payment.getOrderCode()
            );
        } finally {
            if (paidPayment != null) cleanup(paidPayment);
            if (payment != null) cleanup(payment);
            TenantContext.clear();
        }
    }

    private static long findAdmin() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM app_users WHERE role='ADMIN' AND active LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new IllegalStateException("No active ADMIN.");
            return resultSet.getLong(1);
        }
    }

    private static ProductSnapshot findProduct() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id,code,stock_quantity FROM products "
                             + "WHERE active AND stock_quantity>0 LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new IllegalStateException("No product in stock.");
            return new ProductSnapshot(
                    resultSet.getLong("id"),
                    resultSet.getString("code"),
                    resultSet.getInt("stock_quantity"));
        }
    }

    private static int stock(long productId) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT stock_quantity FROM products WHERE id=?")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static void cleanup(PaymentTransaction payment) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, "DELETE FROM stock_transactions "
                        + "WHERE reference_type='INVOICE' AND reference_id=?",
                        payment.getInvoiceId());
                execute(connection, "DELETE FROM payment_transactions WHERE invoice_id=?",
                        payment.getInvoiceId());
                execute(connection, "DELETE FROM invoice_details WHERE invoice_id=?",
                        payment.getInvoiceId());
                execute(connection, "DELETE FROM invoices WHERE id=?",
                        payment.getInvoiceId());
                execute(connection, "DELETE FROM payment_webhook_routes WHERE order_code=?",
                        payment.getOrderCode());
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void execute(Connection connection, String sql, long value)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, value);
            statement.executeUpdate();
        }
    }

    private static final class ProductSnapshot {
        private final long id;
        private final String code;
        private final int stock;

        private ProductSnapshot(long id, String code, int stock) {
            this.id = id;
            this.code = code;
            this.stock = stock;
        }
    }
}
