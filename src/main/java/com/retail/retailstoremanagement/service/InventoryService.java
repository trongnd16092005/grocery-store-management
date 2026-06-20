package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.InventoryDao;
import com.retail.retailstoremanagement.dao.impl.JdbcInventoryDao;
import com.retail.retailstoremanagement.model.StockTransaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class InventoryService {
    private final InventoryDao inventoryDao;

    public InventoryService() {
        this(new JdbcInventoryDao());
    }

    public InventoryService(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public List<StockTransaction> findRecentTransactions(int limit) throws SQLException {
        return inventoryDao.findRecentTransactions(Math.max(1, Math.min(limit, 100)));
    }

    public void importStock(long productId, int quantity, BigDecimal unitCost,
                            long supplierId, String note, Long userId) throws SQLException {
        if (productId <= 0 || supplierId <= 0) {
            throw new ValidationException("Sản phẩm và nhà cung cấp không hợp lệ.");
        }
        if (quantity <= 0) {
            throw new ValidationException("Số lượng nhập phải lớn hơn 0.");
        }
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá nhập không được âm.");
        }
        inventoryDao.importStock(productId, quantity, unitCost, supplierId, note, userId);
    }

    public void adjustStock(long productId, int newStock, String reason, Long userId)
            throws SQLException {
        if (productId <= 0) {
            throw new ValidationException("Sản phẩm không hợp lệ.");
        }
        if (newStock < 0) {
            throw new ValidationException("Tồn kho mới không được âm.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Vui lòng nhập lý do điều chỉnh.");
        }
        inventoryDao.adjustStock(productId, newStock, reason.trim(), userId);
    }
}
