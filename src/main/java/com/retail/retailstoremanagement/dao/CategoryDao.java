package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.Category;

import java.sql.SQLException;
import java.util.List;

public interface CategoryDao extends BaseDao<Category> {
    List<Category> search(String keyword) throws SQLException;

    boolean nameExists(String name, Long excludedId) throws SQLException;

    boolean softDelete(long id) throws SQLException;
}
