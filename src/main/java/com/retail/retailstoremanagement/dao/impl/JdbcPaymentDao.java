package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.PaymentDao;
import com.retail.retailstoremanagement.model.DiscountCode;
import com.retail.retailstoremanagement.model.CustomerType;
import com.retail.retailstoremanagement.model.InvoiceDetail;
import com.retail.retailstoremanagement.model.PaymentStatus;
import com.retail.retailstoremanagement.model.PaymentTransaction;
import com.retail.retailstoremanagement.service.DiscountCodeService;
import com.retail.retailstoremanagement.service.ValidationException;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TenantContext;

import java.math.BigDecimal;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcPaymentDao implements PaymentDao {
    private final JdbcDiscountCodeDao discountDao = new JdbcDiscountCodeDao();
    private final DiscountCodeService discountService = new DiscountCodeService(discountDao);

    public PaymentTransaction createPending(Map<String, Integer> items, String customerCode,
                                            String discountCode, Long cashierId,
                                            OffsetDateTime expiresAt) throws SQLException {
        return createPending(items, customerCode, discountCode, 0, null, cashierId, expiresAt);
    }

    @Override
    public PaymentTransaction createPending(Map<String, Integer> items, String customerCode,
                                            String discountCode, int pointsToRedeem,
                                            BigDecimal providerAmount, Long cashierId,
                                            OffsetDateTime expiresAt) throws SQLException {
        Long storeId = TenantContext.getStoreId();
        if (storeId == null) throw new SQLException("Không xác định được cửa hàng.");
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                CustomerInfo customer = findCustomer(connection, customerCode);
                List<InvoiceDetail> details = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (Map.Entry<String, Integer> item : items.entrySet()) {
                    InvoiceDetail detail = lockProduct(
                            connection, item.getKey(), item.getValue());
                    details.add(detail);
                    subtotal = subtotal.add(detail.calculateLineTotal());
                }

                DiscountCode appliedCode = null;
                BigDecimal discountAmount = BigDecimal.ZERO;
                if (discountCode != null && !discountCode.isBlank()) {
                    appliedCode = discountDao.lockByCode(connection, discountCode)
                            .orElseThrow(() -> new SQLException("Mã giảm giá không tồn tại."));
                    try {
                        discountAmount = discountService.calculateValidDiscount(
                                appliedCode, subtotal, OffsetDateTime.now(),
                                customer == null ? null : customer.type,
                                eligibleSubtotal(details, appliedCode));
                    } catch (RuntimeException exception) {
                        throw new SQLException(exception.getMessage(), exception);
                    }
                }
                PointUsage pointUsage = calculatePointUsage(
                        customer, pointsToRedeem, subtotal.subtract(discountAmount));
                discountAmount = discountAmount.add(pointUsage.discount);
                BigDecimal total = subtotal.subtract(discountAmount);
                int pointsEarned = customer == null
                        ? 0 : total.divideToIntegralValue(BigDecimal.valueOf(10000)).intValue();
                if (total.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new SQLException(
                            "Đơn hàng 0 đồng không thể thanh toán qua payOS.");
                }
                try {
                    total.longValueExact();
                } catch (ArithmeticException exception) {
                    throw new SQLException(
                            "payOS chỉ hỗ trợ số tiền VND nguyên.", exception);
                }

                long invoiceId = insertInvoice(connection, customer == null ? null : customer.id, cashierId,
                        subtotal, discountAmount, total, appliedCode,
                        pointUsage.points, pointUsage.discount, pointsEarned);
                for (InvoiceDetail detail : details) {
                    insertDetail(connection, invoiceId, detail);
                    holdStock(connection, detail, invoiceId, cashierId);
                }
                if (appliedCode != null) discountDao.incrementUsage(
                        connection, appliedCode.getId());
                if (customer != null && pointUsage.points > 0) {
                    reservePoints(connection, customer.id, pointUsage.points);
                }

                long orderCode = nextOrderCode(connection);
                BigDecimal paymentAmount = providerAmount == null ? total : providerAmount;
                insertPayment(connection, invoiceId, orderCode, paymentAmount, expiresAt);
                insertRoute(connection, orderCode, storeId);
                connection.commit();
                return findByOrderCode(orderCode);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException("Không thể khởi tạo giao dịch QR.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public PaymentTransaction attachPayOs(long orderCode, String paymentLinkId,
                                          String checkoutUrl, String qrCode)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE payment_transactions SET payment_link_id=?,checkout_url=?,qr_code=? "
                             + "WHERE order_code=? AND status='PENDING' RETURNING *")) {
            statement.setString(1, paymentLinkId);
            statement.setString(2, checkoutUrl);
            statement.setString(3, qrCode);
            statement.setLong(4, orderCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Giao dịch QR không còn hiệu lực.");
                return map(resultSet);
            }
        }
    }

    @Override
    public PaymentTransaction findByInvoice(long invoiceId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT p.*,i.code invoice_code,i.total_amount invoice_total_amount FROM payment_transactions p "
                             + "JOIN invoices i ON i.id=p.invoice_id WHERE p.invoice_id=?")) {
            statement.setLong(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public Long findStoreIdByOrderCode(long orderCode) throws SQLException {
        Long previous = TenantContext.getStoreId();
        TenantContext.clear();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT store_id FROM payment_webhook_routes WHERE order_code=?")) {
            statement.setLong(1, orderCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        } finally {
            if (previous != null) TenantContext.setStoreId(previous);
        }
    }

    @Override
    public PaymentTransaction markPaid(long orderCode, BigDecimal amount,
                                       String providerReference, String rawWebhook)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PaymentTransaction payment = lockPayment(connection, orderCode);
                if (payment.getStatus() == PaymentStatus.PAID) {
                    connection.commit();
                    return payment;
                }
                if (payment.getStatus() != PaymentStatus.PENDING) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE payment_transactions SET status='REVIEW',"
                                    + "provider_reference=?,raw_webhook=?,failure_reason=? "
                                    + "WHERE order_code=?")) {
                        statement.setString(1, providerReference);
                        statement.setString(2, rawWebhook);
                        statement.setString(3,
                                "payOS báo đã thanh toán sau khi giao dịch không còn PENDING.");
                        statement.setLong(4, orderCode);
                        statement.executeUpdate();
                    }
                    connection.commit();
                    return findByOrderCode(orderCode);
                }
                if (payment.getAmount().compareTo(amount) != 0) {
                    throw new SQLException("Số tiền webhook không khớp giao dịch.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE payment_transactions SET status='PAID',paid_at=CURRENT_TIMESTAMP,"
                                + "provider_reference=?,raw_webhook=? WHERE order_code=?")) {
                    statement.setString(1, providerReference);
                    statement.setString(2, rawWebhook);
                    statement.setLong(3, orderCode);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE invoices SET status='PAID' WHERE id=? AND status='PENDING'")) {
                    statement.setLong(1, payment.getInvoiceId());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Hóa đơn QR không còn ở trạng thái chờ.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE stock_transactions SET transaction_type='SALE',"
                                + "reason='Thanh toán QR thành công' "
                                + "WHERE reference_type='INVOICE' AND reference_id=? "
                                + "AND transaction_type='PAYMENT_HOLD'")) {
                    statement.setLong(1, payment.getInvoiceId());
                    statement.executeUpdate();
                }
                Long customerId = invoiceCustomerId(connection, payment.getInvoiceId());
                if (customerId != null) awardPendingInvoicePoints(
                        connection, payment.getInvoiceId(), customerId);
                connection.commit();
                return findByOrderCode(orderCode);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException(exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public PaymentTransaction release(long orderCode, PaymentStatus status,
                                      String reason) throws SQLException {
        if (status != PaymentStatus.CANCELLED
                && status != PaymentStatus.EXPIRED
                && status != PaymentStatus.FAILED) {
            throw new IllegalArgumentException("Trạng thái giải phóng giao dịch không hợp lệ.");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PaymentTransaction payment = lockPayment(connection, orderCode);
                if (payment.getStatus() != PaymentStatus.PENDING) {
                    connection.commit();
                    return payment;
                }
                releaseStock(connection, payment.getInvoiceId(), reason);
                decrementDiscountUsage(connection, payment.getInvoiceId());
                restoreReservedPoints(connection, payment.getInvoiceId());
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE invoices SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP "
                                + "WHERE id=? AND status='PENDING'")) {
                    statement.setLong(1, payment.getInvoiceId());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE payment_transactions SET status=?,cancelled_at=CURRENT_TIMESTAMP,"
                                + "failure_reason=? WHERE order_code=?")) {
                    statement.setString(1, status.name());
                    statement.setString(2, reason);
                    statement.setLong(3, orderCode);
                    statement.executeUpdate();
                }
                connection.commit();
                return findByOrderCode(orderCode);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException(exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private CustomerInfo findCustomer(Connection connection, String code) throws SQLException {
        if (code == null) return null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id,customer_type,loyalty_points FROM customers "
                        + "WHERE UPPER(code)=? AND active FOR UPDATE")) {
            statement.setString(1, code.toUpperCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Không tìm thấy khách hàng " + code + ".");
                }
                return new CustomerInfo(
                        resultSet.getLong("id"),
                        CustomerType.valueOf(resultSet.getString("customer_type")),
                        resultSet.getInt("loyalty_points"));
            }
        }
    }

    private BigDecimal eligibleSubtotal(List<InvoiceDetail> details, DiscountCode code) {
        if (code == null || code.getProductId() == null) return null;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceDetail detail : details) {
            if (code.getProductId().equals(detail.getProductId())) {
                total = total.add(detail.calculateLineTotal());
            }
        }
        return total;
    }

    private InvoiceDetail lockProduct(Connection connection, String code, int quantity)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id,code,name,selling_price,stock_quantity FROM products "
                        + "WHERE UPPER(code)=? AND active FOR UPDATE")) {
            statement.setString(1, code.toUpperCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Sản phẩm " + code
                            + " không tồn tại hoặc đã ngừng bán.");
                }
                int stock = resultSet.getInt("stock_quantity");
                if (stock < quantity) {
                    throw new SQLException("Sản phẩm " + resultSet.getString("name")
                            + " chỉ còn " + stock + " trong kho.");
                }
                InvoiceDetail detail = new InvoiceDetail();
                detail.setProductId(resultSet.getLong("id"));
                detail.setProductCode(resultSet.getString("code"));
                detail.setProductName(resultSet.getString("name"));
                detail.setUnitPrice(resultSet.getBigDecimal("selling_price"));
                detail.setQuantity(quantity);
                detail.setLineTotal(detail.calculateLineTotal());
                return detail;
            }
        }
    }

    private long insertInvoice(Connection connection, Long customerId, Long cashierId,
                               BigDecimal subtotal, BigDecimal discountAmount,
                               BigDecimal total, DiscountCode discountCode,
                               int pointsRedeemed, BigDecimal pointsDiscount,
                               int pointsEarned)
            throws SQLException {
        String sql = "INSERT INTO invoices(code,customer_id,cashier_id,payment_method,status,"
                + "subtotal,discount_amount,total_amount,discount_code_id,discount_code,"
                + "points_redeemed,points_discount_amount,points_earned) "
                + "VALUES ('HD'||LPAD(nextval('invoice_code_seq')::text,3,'0'),"
                + "?,?,'QR','PENDING',?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            nullableLong(statement, 1, customerId);
            nullableLong(statement, 2, cashierId);
            statement.setBigDecimal(3, subtotal);
            statement.setBigDecimal(4, discountAmount);
            statement.setBigDecimal(5, total);
            nullableLong(statement, 6, discountCode == null ? null : discountCode.getId());
            statement.setString(7, discountCode == null ? null : discountCode.getCode());
            statement.setInt(8, pointsRedeemed);
            statement.setBigDecimal(9, pointsDiscount);
            statement.setInt(10, pointsEarned);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertDetail(Connection connection, long invoiceId, InvoiceDetail detail)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO invoice_details(invoice_id,product_id,product_code,product_name,"
                        + "unit_price,quantity) VALUES (?,?,?,?,?,?)")) {
            statement.setLong(1, invoiceId);
            statement.setLong(2, detail.getProductId());
            statement.setString(3, detail.getProductCode());
            statement.setString(4, detail.getProductName());
            statement.setBigDecimal(5, detail.getUnitPrice());
            statement.setInt(6, detail.getQuantity());
            statement.executeUpdate();
        }
    }

    private void holdStock(Connection connection, InvoiceDetail detail,
                           long invoiceId, Long cashierId) throws SQLException {
        int before;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_quantity FROM products WHERE id=? FOR UPDATE")) {
            statement.setLong(1, detail.getProductId());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                before = resultSet.getInt(1);
            }
        }
        int after = before - detail.getQuantity();
        if (after < 0) throw new SQLException("Tồn kho vừa thay đổi, vui lòng thử lại.");
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET stock_quantity=? WHERE id=?")) {
            statement.setInt(1, after);
            statement.setLong(2, detail.getProductId());
            statement.executeUpdate();
        }
        insertStockLog(connection, detail.getProductId(), "PAYMENT_HOLD",
                -detail.getQuantity(), before, after, invoiceId,
                "Giữ hàng chờ thanh toán QR", cashierId);
    }

    private void releaseStock(Connection connection, long invoiceId, String reason)
            throws SQLException {
        try (PreparedStatement details = connection.prepareStatement(
                "SELECT product_id,quantity FROM invoice_details "
                        + "WHERE invoice_id=? AND product_id IS NOT NULL ORDER BY id")) {
            details.setLong(1, invoiceId);
            try (ResultSet resultSet = details.executeQuery()) {
                while (resultSet.next()) {
                    long productId = resultSet.getLong("product_id");
                    int quantity = resultSet.getInt("quantity");
                    int before;
                    try (PreparedStatement lock = connection.prepareStatement(
                            "SELECT stock_quantity FROM products WHERE id=? FOR UPDATE")) {
                        lock.setLong(1, productId);
                        try (ResultSet stock = lock.executeQuery()) {
                            if (!stock.next()) continue;
                            before = stock.getInt(1);
                        }
                    }
                    int after = before + quantity;
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE products SET stock_quantity=? WHERE id=?")) {
                        update.setInt(1, after);
                        update.setLong(2, productId);
                        update.executeUpdate();
                    }
                    insertStockLog(connection, productId, "PAYMENT_RELEASE",
                            quantity, before, after, invoiceId, reason, null);
                }
            }
        }
    }

    private void decrementDiscountUsage(Connection connection, long invoiceId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE discount_codes SET used_count=GREATEST(used_count-1,0) "
                        + "WHERE id=(SELECT discount_code_id FROM invoices WHERE id=?)")) {
            statement.setLong(1, invoiceId);
            statement.executeUpdate();
        }
    }

    private PointUsage calculatePointUsage(CustomerInfo customer, int requested,
                                           BigDecimal amountBeforePoints) {
        if (requested <= 0) return new PointUsage(0, BigDecimal.ZERO);
        if (customer == null) {
            throw new ValidationException("Cần chọn khách hàng để sử dụng điểm.");
        }
        if (requested > customer.availablePoints) {
            throw new ValidationException("Khách hàng không đủ điểm hiện có.");
        }
        int maximumByAmount = amountBeforePoints
                .divideToIntegralValue(BigDecimal.valueOf(100)).intValue();
        if (requested > maximumByAmount) {
            throw new ValidationException("Số điểm vượt quá giá trị đơn hàng.");
        }
        return new PointUsage(requested, BigDecimal.valueOf(requested * 100L));
    }

    private void reservePoints(Connection connection, long customerId, int points)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE customers SET loyalty_points=loyalty_points-? "
                        + "WHERE id=? AND loyalty_points>=?")) {
            statement.setInt(1, points);
            statement.setLong(2, customerId);
            statement.setInt(3, points);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Điểm khách hàng vừa thay đổi.");
            }
        }
    }

    private void restoreReservedPoints(Connection connection, long invoiceId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE customers SET loyalty_points=loyalty_points+i.points_redeemed "
                        + "FROM invoices i WHERE i.customer_id=customers.id "
                        + "AND i.id=? AND i.points_redeemed>0")) {
            statement.setLong(1, invoiceId);
            statement.executeUpdate();
        }
    }

    private void awardPendingInvoicePoints(Connection connection, long invoiceId,
                                           long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE customers SET loyalty_points=loyalty_points+i.points_earned,"
                        + "lifetime_loyalty_points=lifetime_loyalty_points+i.points_earned,"
                        + "customer_type=CASE "
                        + "WHEN lifetime_loyalty_points+i.points_earned>=300 THEN 'VIP' "
                        + "WHEN lifetime_loyalty_points+i.points_earned>=50 THEN 'LOYAL' "
                        + "ELSE 'REGULAR' END "
                        + "FROM invoices i WHERE customers.id=? AND i.id=?")) {
            statement.setLong(1, customerId);
            statement.setLong(2, invoiceId);
            statement.executeUpdate();
        }
    }

    private Long invoiceCustomerId(Connection connection, long invoiceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT customer_id FROM invoices WHERE id=?")) {
            statement.setLong(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                long value = resultSet.getLong(1);
                return resultSet.wasNull() ? null : value;
            }
        }
    }

    private void insertStockLog(Connection connection, long productId, String type,
                                int change, int before, int after, long invoiceId,
                                String reason, Long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_transactions(product_id,transaction_type,quantity_change,"
                        + "stock_before,stock_after,reference_type,reference_id,reason,created_by) "
                        + "VALUES (?,?,?,?,?,'INVOICE',?,?,?)")) {
            statement.setLong(1, productId);
            statement.setString(2, type);
            statement.setInt(3, change);
            statement.setInt(4, before);
            statement.setInt(5, after);
            statement.setLong(6, invoiceId);
            statement.setString(7, reason);
            nullableLong(statement, 8, userId);
            statement.executeUpdate();
        }
    }

    private long nextOrderCode(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT nextval('payment_order_code_seq')")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private void insertPayment(Connection connection, long invoiceId, long orderCode,
                               BigDecimal amount, OffsetDateTime expiresAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO payment_transactions(invoice_id,order_code,amount,expires_at) "
                        + "VALUES (?,?,?,?)")) {
            statement.setLong(1, invoiceId);
            statement.setLong(2, orderCode);
            statement.setBigDecimal(3, amount);
            statement.setObject(4, expiresAt);
            statement.executeUpdate();
        }
    }

    private void insertRoute(Connection connection, long orderCode, long storeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO payment_webhook_routes(order_code,store_id) VALUES (?,?)")) {
            statement.setLong(1, orderCode);
            statement.setLong(2, storeId);
            statement.executeUpdate();
        }
    }

    private PaymentTransaction lockPayment(Connection connection, long orderCode)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT p.*,i.code invoice_code,i.total_amount invoice_total_amount FROM payment_transactions p "
                        + "JOIN invoices i ON i.id=p.invoice_id "
                        + "WHERE p.order_code=? FOR UPDATE OF p")) {
            statement.setLong(1, orderCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Không tìm thấy giao dịch QR.");
                return map(resultSet);
            }
        }
    }

    private PaymentTransaction findByOrderCode(long orderCode) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT p.*,i.code invoice_code,i.total_amount invoice_total_amount FROM payment_transactions p "
                             + "JOIN invoices i ON i.id=p.invoice_id WHERE p.order_code=?")) {
            statement.setLong(1, orderCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Không tìm thấy giao dịch QR.");
                return map(resultSet);
            }
        }
    }

    private PaymentTransaction map(ResultSet resultSet) throws SQLException {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setId(resultSet.getLong("id"));
        payment.setInvoiceId(resultSet.getLong("invoice_id"));
        try { payment.setInvoiceCode(resultSet.getString("invoice_code")); }
        catch (SQLException ignored) { }
        try { payment.setInvoiceTotalAmount(resultSet.getBigDecimal("invoice_total_amount")); }
        catch (SQLException ignored) { }
        payment.setOrderCode(resultSet.getLong("order_code"));
        payment.setPaymentLinkId(resultSet.getString("payment_link_id"));
        payment.setProviderReference(resultSet.getString("provider_reference"));
        payment.setAmount(resultSet.getBigDecimal("amount"));
        payment.setStatus(PaymentStatus.valueOf(resultSet.getString("status")));
        payment.setCheckoutUrl(resultSet.getString("checkout_url"));
        payment.setQrCode(resultSet.getString("qr_code"));
        payment.setExpiresAt(resultSet.getObject("expires_at", OffsetDateTime.class));
        payment.setPaidAt(resultSet.getObject("paid_at", OffsetDateTime.class));
        payment.setFailureReason(resultSet.getString("failure_reason"));
        payment.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        payment.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return payment;
    }

    private void nullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value);
    }

    private static final class CustomerInfo {
        private final long id;
        private final CustomerType type;
        private final int availablePoints;

        private CustomerInfo(long id, CustomerType type, int availablePoints) {
            this.id = id;
            this.type = type;
            this.availablePoints = availablePoints;
        }
    }

    private static final class PointUsage {
        private final int points;
        private final BigDecimal discount;

        private PointUsage(int points, BigDecimal discount) {
            this.points = points;
            this.discount = discount;
        }
    }
}
