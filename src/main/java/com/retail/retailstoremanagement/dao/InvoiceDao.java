package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.model.PaymentMethod;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface InvoiceDao {
    Invoice checkout(Map<String, Integer> items, String customerCode, PaymentMethod paymentMethod,
                     BigDecimal cashReceived, Long cashierId) throws SQLException;
    List<Invoice> findAll(String keyword, String status, int limit, int offset) throws SQLException;
    Invoice findById(long id) throws SQLException;
    void cancel(long id, Long userId) throws SQLException;
}
