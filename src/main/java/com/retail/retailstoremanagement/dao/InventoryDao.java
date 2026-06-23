package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.StockTransaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public interface InventoryDao {
    List<StockTransaction> findRecentTransactions(int limit) throws SQLException;

    void importStock(long productId, int quantity, BigDecimal unitCost,
                     long supplierId, String note, Long userId) throws SQLException;

    void adjustStock(long productId, int newStock, String reason,
                     Long userId) throws SQLException;
}
