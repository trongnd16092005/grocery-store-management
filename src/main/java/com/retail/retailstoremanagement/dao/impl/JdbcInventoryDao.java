package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.InventoryDao;
import com.retail.retailstoremanagement.model.StockTransaction;
import com.retail.retailstoremanagement.model.StockTransactionType;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcInventoryDao implements InventoryDao {
    @Override
    public List<StockTransaction> findRecentTransactions(int limit) throws SQLException {
        String sql = "SELECT st.*, p.code AS product_code, p.name AS product_name "
                + "FROM stock_transactions st JOIN products p ON p.id = st.product_id "
                + "ORDER BY st.created_at DESC LIMIT ?";
        List<StockTransaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(map(resultSet));
                }
            }
        }
        return transactions;
    }

    @Override
    public void importStock(long productId, int quantity, BigDecimal unitCost,
                            long supplierId, String note, Long userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int stockBefore = lockProductStock(connection, productId);
                int stockAfter = Math.addExact(stockBefore, quantity);
                BigDecimal totalAmount = unitCost.multiply(BigDecimal.valueOf(quantity));
                long purchaseOrderId = insertPurchaseOrder(
                        connection, supplierId, totalAmount, note, userId
                );
                insertPurchaseOrderDetail(
                        connection, purchaseOrderId, productId, quantity, unitCost
                );

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE products SET stock_quantity = ?, cost_price = ?, supplier_id = ? WHERE id = ?")) {
                    statement.setInt(1, stockAfter);
                    statement.setBigDecimal(2, unitCost);
                    statement.setLong(3, supplierId);
                    statement.setLong(4, productId);
                    statement.executeUpdate();
                }

                insertStockTransaction(
                        connection, productId, StockTransactionType.IMPORT, quantity,
                        stockBefore, stockAfter, "PURCHASE_ORDER", purchaseOrderId,
                        note, userId
                );
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) {
                    throw (SQLException) exception;
                }
                throw new SQLException("Could not import stock.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void adjustStock(long productId, int newStock, String reason, Long userId)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int stockBefore = lockProductStock(connection, productId);
                int difference = newStock - stockBefore;
                if (difference == 0) {
                    throw new SQLException("New stock equals current stock.");
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE products SET stock_quantity = ? WHERE id = ?")) {
                    statement.setInt(1, newStock);
                    statement.setLong(2, productId);
                    statement.executeUpdate();
                }

                insertStockTransaction(
                        connection, productId, StockTransactionType.ADJUSTMENT,
                        difference, stockBefore, newStock,
                        "STOCKTAKE", null, reason, userId
                );
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException) {
                    throw (SQLException) exception;
                }
                throw new SQLException("Could not adjust stock.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int lockProductStock(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_quantity FROM products WHERE id = ? AND active FOR UPDATE")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Active product was not found: " + productId);
                }
                return resultSet.getInt(1);
            }
        }
    }

    private long insertPurchaseOrder(Connection connection, long supplierId,
                                     BigDecimal totalAmount, String note, Long userId)
            throws SQLException {
        String sql = "INSERT INTO purchase_orders(code, supplier_id, created_by, status, "
                + "total_amount, note, completed_at) "
                + "VALUES ('PN' || LPAD(nextval('purchase_order_code_seq')::text, 3, '0'), "
                + "?, ?, 'COMPLETED', ?, ?, CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, supplierId);
            setNullableLong(statement, 2, userId);
            statement.setBigDecimal(3, totalAmount);
            statement.setString(4, note);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertPurchaseOrderDetail(Connection connection, long purchaseOrderId,
                                           long productId, int quantity, BigDecimal unitCost)
            throws SQLException {
        String sql = "INSERT INTO purchase_order_details(purchase_order_id, product_id, quantity, unit_cost) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, purchaseOrderId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.setBigDecimal(4, unitCost);
            statement.executeUpdate();
        }
    }

    private void insertStockTransaction(Connection connection, long productId,
                                        StockTransactionType type, int quantityChange,
                                        int stockBefore, int stockAfter,
                                        String referenceType, Long referenceId,
                                        String reason, Long userId) throws SQLException {
        String sql = "INSERT INTO stock_transactions(product_id, transaction_type, quantity_change, "
                + "stock_before, stock_after, reference_type, reference_id, reason, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, type.name());
            statement.setInt(3, quantityChange);
            statement.setInt(4, stockBefore);
            statement.setInt(5, stockAfter);
            statement.setString(6, referenceType);
            setNullableLong(statement, 7, referenceId);
            statement.setString(8, reason);
            setNullableLong(statement, 9, userId);
            statement.executeUpdate();
        }
    }

    private StockTransaction map(ResultSet resultSet) throws SQLException {
        StockTransaction transaction = new StockTransaction();
        transaction.setId(resultSet.getLong("id"));
        transaction.setProductId(resultSet.getLong("product_id"));
        transaction.setProductCode(resultSet.getString("product_code"));
        transaction.setProductName(resultSet.getString("product_name"));
        transaction.setTransactionType(
                StockTransactionType.valueOf(resultSet.getString("transaction_type"))
        );
        transaction.setQuantityChange(resultSet.getInt("quantity_change"));
        transaction.setStockBefore(resultSet.getInt("stock_before"));
        transaction.setStockAfter(resultSet.getInt("stock_after"));
        transaction.setReferenceType(resultSet.getString("reference_type"));
        long referenceId = resultSet.getLong("reference_id");
        transaction.setReferenceId(resultSet.wasNull() ? null : referenceId);
        transaction.setReason(resultSet.getString("reason"));
        long createdBy = resultSet.getLong("created_by");
        transaction.setCreatedBy(resultSet.wasNull() ? null : createdBy);
        transaction.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        return transaction;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
