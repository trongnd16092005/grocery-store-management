package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.Product;

import java.sql.SQLException;
import java.util.List;

public interface ProductDao extends BaseDao<Product> {
    List<Product> search(String keyword, Long categoryId, String stockStatus,
                         int limit, int offset) throws SQLException;

    long count(String keyword, Long categoryId, String stockStatus) throws SQLException;

    boolean barcodeExists(String barcode, Long excludedId) throws SQLException;

    boolean softDelete(long id) throws SQLException;
}
