package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.PurchaseOrderDao;
import com.retail.retailstoremanagement.model.PurchaseOrder;
import com.retail.retailstoremanagement.model.PurchaseOrderDetail;
import com.retail.retailstoremanagement.model.PurchaseOrderStatus;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcPurchaseOrderDao implements PurchaseOrderDao {
    private static final String SELECT_BASE =
            "SELECT po.*, s.name supplier_name, u.full_name created_by_name "
                    + "FROM purchase_orders po "
                    + "JOIN suppliers s ON s.id=po.supplier_id "
                    + "LEFT JOIN app_users u ON u.id=po.created_by ";

    @Override
    public List<PurchaseOrder> findAll(String keyword, String status, int limit, int offset)
            throws SQLException {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        String sql = SELECT_BASE
                + "WHERE (?='' OR LOWER(po.code) LIKE LOWER(?) OR LOWER(s.name) LIKE LOWER(?)) "
                + "AND (?='' OR po.status=?) ORDER BY po.created_at DESC LIMIT ? OFFSET ?";
        List<PurchaseOrder> orders = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, "%" + normalized + "%");
            statement.setString(3, "%" + normalized + "%");
            statement.setString(4, normalizedStatus);
            statement.setString(5, normalizedStatus);
            statement.setInt(6, limit);
            statement.setInt(7, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) orders.add(mapOrder(resultSet));
            }
        }
        return orders;
    }

    @Override
    public PurchaseOrder findById(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SELECT_BASE + "WHERE po.id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                PurchaseOrder order = mapOrder(resultSet);
                order.setDetails(findDetails(connection, id));
                return order;
            }
        }
    }

    @Override
    public PurchaseOrder createDraft(long supplierId, String note, Long userId,
                                     List<PurchaseOrderDetail> details) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureActiveSupplier(connection, supplierId);
                BigDecimal total = BigDecimal.ZERO;
                for (PurchaseOrderDetail detail : details) {
                    ensureActiveProduct(connection, detail.getProductId());
                    total = total.add(detail.calculateLineTotal());
                }
                long orderId;
                String sql = "INSERT INTO purchase_orders(code,supplier_id,created_by,status,"
                        + "total_amount,note) VALUES "
                        + "('PN'||LPAD(nextval('purchase_order_code_seq')::text,3,'0'),"
                        + "?,?,'DRAFT',?,?) RETURNING id";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, supplierId);
                    setNullableLong(statement, 2, userId);
                    statement.setBigDecimal(3, total);
                    statement.setString(4, note);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        orderId = resultSet.getLong(1);
                    }
                }
                for (PurchaseOrderDetail detail : details) {
                    insertDetail(connection, orderId, detail);
                }
                connection.commit();
                return findById(orderId);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException("Không thể tạo phiếu nhập.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void complete(long id, Long userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PurchaseOrder order = lockOrder(connection, id);
                if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
                    throw new SQLException("Chỉ phiếu nháp mới có thể hoàn thành.");
                }
                List<PurchaseOrderDetail> details = findDetails(connection, id);
                if (details.isEmpty()) throw new SQLException("Phiếu nhập chưa có sản phẩm.");
                for (PurchaseOrderDetail detail : details) {
                    int before = lockStock(connection, detail.getProductId());
                    int after = Math.addExact(before, detail.getQuantity());
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE products SET stock_quantity=?,cost_price=?,supplier_id=? WHERE id=?")) {
                        statement.setInt(1, after);
                        statement.setBigDecimal(2, detail.getUnitCost());
                        statement.setLong(3, order.getSupplierId());
                        statement.setLong(4, detail.getProductId());
                        statement.executeUpdate();
                    }
                    insertStockLog(connection, detail.getProductId(), "IMPORT",
                            detail.getQuantity(), before, after, id,
                            "Hoàn thành phiếu nhập " + order.getCode(), userId);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE purchase_orders SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP "
                                + "WHERE id=?")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException("Không thể hoàn thành phiếu nhập.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void cancel(long id, Long userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PurchaseOrder order = lockOrder(connection, id);
                if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
                    throw new SQLException("Phiếu nhập đã được hủy.");
                }
                if (order.getStatus() == PurchaseOrderStatus.COMPLETED) {
                    for (PurchaseOrderDetail detail : findDetails(connection, id)) {
                        int before = lockStock(connection, detail.getProductId());
                        if (before < detail.getQuantity()) {
                            throw new SQLException("Không thể hủy vì tồn kho của "
                                    + detail.getProductName() + " không đủ để hoàn tác.");
                        }
                        int after = before - detail.getQuantity();
                        try (PreparedStatement statement = connection.prepareStatement(
                                "UPDATE products SET stock_quantity=? WHERE id=?")) {
                            statement.setInt(1, after);
                            statement.setLong(2, detail.getProductId());
                            statement.executeUpdate();
                        }
                        insertStockLog(connection, detail.getProductId(), "CANCEL_IMPORT",
                                -detail.getQuantity(), before, after, id,
                                "Hủy phiếu nhập " + order.getCode(), userId);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE purchase_orders SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP,"
                                + "cancelled_by=? WHERE id=?")) {
                    setNullableLong(statement, 1, userId);
                    statement.setLong(2, id);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) throw (SQLException) exception;
                throw new SQLException("Không thể hủy phiếu nhập.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private PurchaseOrder lockOrder(Connection connection, long id) throws SQLException {
        String sql = "SELECT po.*,s.name supplier_name,u.full_name created_by_name "
                + "FROM purchase_orders po JOIN suppliers s ON s.id=po.supplier_id "
                + "LEFT JOIN app_users u ON u.id=po.created_by WHERE po.id=? FOR UPDATE OF po";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Không tìm thấy phiếu nhập.");
                return mapOrder(resultSet);
            }
        }
    }

    private List<PurchaseOrderDetail> findDetails(Connection connection, long orderId)
            throws SQLException {
        String sql = "SELECT pod.*,p.code product_code,p.name product_name "
                + "FROM purchase_order_details pod JOIN products p ON p.id=pod.product_id "
                + "WHERE pod.purchase_order_id=? ORDER BY pod.id";
        List<PurchaseOrderDetail> details = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    PurchaseOrderDetail detail = new PurchaseOrderDetail();
                    detail.setId(resultSet.getLong("id"));
                    detail.setPurchaseOrderId(orderId);
                    detail.setProductId(resultSet.getLong("product_id"));
                    detail.setProductCode(resultSet.getString("product_code"));
                    detail.setProductName(resultSet.getString("product_name"));
                    detail.setQuantity(resultSet.getInt("quantity"));
                    detail.setUnitCost(resultSet.getBigDecimal("unit_cost"));
                    detail.setLineTotal(resultSet.getBigDecimal("line_total"));
                    details.add(detail);
                }
            }
        }
        return details;
    }

    private void insertDetail(Connection connection, long orderId,
                              PurchaseOrderDetail detail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO purchase_order_details(purchase_order_id,product_id,quantity,unit_cost) "
                        + "VALUES (?,?,?,?)")) {
            statement.setLong(1, orderId);
            statement.setLong(2, detail.getProductId());
            statement.setInt(3, detail.getQuantity());
            statement.setBigDecimal(4, detail.getUnitCost());
            statement.executeUpdate();
        }
    }

    private void ensureActiveSupplier(Connection connection, long supplierId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM suppliers WHERE id=? AND active")) {
            statement.setLong(1, supplierId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Nhà cung cấp không hợp lệ.");
            }
        }
    }

    private void ensureActiveProduct(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM products WHERE id=? AND active")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Sản phẩm không hợp lệ.");
            }
        }
    }

    private int lockStock(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_quantity FROM products WHERE id=? AND active FOR UPDATE")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Sản phẩm không còn hoạt động.");
                return resultSet.getInt(1);
            }
        }
    }

    private void insertStockLog(Connection connection, long productId, String type,
                                int change, int before, int after, long orderId,
                                String reason, Long userId) throws SQLException {
        String sql = "INSERT INTO stock_transactions(product_id,transaction_type,quantity_change,"
                + "stock_before,stock_after,reference_type,reference_id,reason,created_by) "
                + "VALUES (?,?,?,?,?,'PURCHASE_ORDER',?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, type);
            statement.setInt(3, change);
            statement.setInt(4, before);
            statement.setInt(5, after);
            statement.setLong(6, orderId);
            statement.setString(7, reason);
            setNullableLong(statement, 8, userId);
            statement.executeUpdate();
        }
    }

    private PurchaseOrder mapOrder(ResultSet resultSet) throws SQLException {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(resultSet.getLong("id"));
        order.setCode(resultSet.getString("code"));
        order.setSupplierId(resultSet.getLong("supplier_id"));
        order.setSupplierName(resultSet.getString("supplier_name"));
        long createdBy = resultSet.getLong("created_by");
        order.setCreatedBy(resultSet.wasNull() ? null : createdBy);
        order.setCreatedByName(resultSet.getString("created_by_name"));
        order.setStatus(PurchaseOrderStatus.valueOf(resultSet.getString("status")));
        order.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        order.setNote(resultSet.getString("note"));
        order.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        order.setCompletedAt(resultSet.getObject("completed_at", OffsetDateTime.class));
        order.setCancelledAt(resultSet.getObject("cancelled_at", OffsetDateTime.class));
        return order;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value);
    }
}
