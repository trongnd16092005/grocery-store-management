package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.PurchaseOrder;
import com.retail.retailstoremanagement.model.PurchaseOrderDetail;

import java.sql.SQLException;
import java.util.List;

public interface PurchaseOrderDao {
    List<PurchaseOrder> findAll(String keyword, String status, int limit, int offset)
            throws SQLException;

    PurchaseOrder findById(long id) throws SQLException;

    PurchaseOrder createDraft(long supplierId, String note, Long userId,
                              List<PurchaseOrderDetail> details) throws SQLException;

    void complete(long id, Long userId) throws SQLException;

    void cancel(long id, Long userId) throws SQLException;
}
